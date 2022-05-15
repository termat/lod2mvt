package net.termat.geo.lod2mvt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.locationtech.jts.algorithm.Area;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateArrays;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.wdtinc.mapbox_vector_tile.VectorTile;
import com.wdtinc.mapbox_vector_tile.adapt.jts.IUserDataConverter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.JtsAdapter;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerProps;
import com.wdtinc.mapbox_vector_tile.encoding.GeomCmd;
import com.wdtinc.mapbox_vector_tile.encoding.GeomCmdHdr;
import com.wdtinc.mapbox_vector_tile.encoding.MvtUtil;
import com.wdtinc.mapbox_vector_tile.encoding.ZigZag;
import com.wdtinc.mapbox_vector_tile.util.Vec2d;

public class JtsAdapterReverse {

	public static List<Geometry> flatFeatureList(Geometry geom) {
		final List<Geometry> singleGeoms = new ArrayList<>();
		final Stack<Geometry> geomStack = new Stack<>();
		Geometry nextGeom;
		int nextGeomCount;
		geomStack.push(geom);
		while(!geomStack.isEmpty()) {
			nextGeom = geomStack.pop();
			if(nextGeom instanceof Point
					|| nextGeom instanceof MultiPoint
					|| nextGeom instanceof LineString
					|| nextGeom instanceof MultiLineString
					|| nextGeom instanceof Polygon
					|| nextGeom instanceof MultiPolygon) {
				singleGeoms.add(nextGeom);
			} else if(nextGeom instanceof GeometryCollection) {
				nextGeomCount = nextGeom.getNumGeometries();
				for(int i = 0; i < nextGeomCount; ++i) {
					geomStack.push(nextGeom.getGeometryN(i));
				}
			}
		}
		return singleGeoms;
	}

	public static List<VectorTile.Tile.Feature> toFeatures(Geometry geometry,MvtLayerProps layerProps,
			IUserDataConverter userDataConverter) {
		return toFeatures(flatFeatureList(geometry), layerProps, userDataConverter);
	}

	public static List<VectorTile.Tile.Feature> toFeatures(Collection<Geometry> flatGeoms,MvtLayerProps layerProps,
			IUserDataConverter userDataConverter) {
		if(flatGeoms.isEmpty()) {
			return Collections.emptyList();
		}
		final List<VectorTile.Tile.Feature> features = new ArrayList<>();
		final Vec2d cursor = new Vec2d();
		VectorTile.Tile.Feature nextFeature;
		for(Geometry nextGeom : flatGeoms) {
			cursor.set(0d, 0d);
			nextFeature = toFeature(nextGeom, cursor, layerProps, userDataConverter);
			if(nextFeature != null) {
				features.add(nextFeature);
			}
		}
		return features;
	}

	private static VectorTile.Tile.Feature toFeature(Geometry geom,Vec2d cursor,
			MvtLayerProps layerProps,IUserDataConverter userDataConverter) {
		final VectorTile.Tile.GeomType mvtGeomType = JtsAdapter.toGeomType(geom);
		if(mvtGeomType == VectorTile.Tile.GeomType.UNKNOWN) {
			return null;
		}
		final VectorTile.Tile.Feature.Builder featureBuilder = VectorTile.Tile.Feature.newBuilder();
		final boolean mvtClosePath = MvtUtil.shouldClosePath(mvtGeomType);
		final List<Integer> mvtGeom = new ArrayList<>();
		featureBuilder.setType(mvtGeomType);
		if(geom instanceof Point || geom instanceof MultiPoint) {
			mvtGeom.addAll(ptsToGeomCmds(geom, cursor));
		} else if(geom instanceof LineString || geom instanceof MultiLineString) {
			for (int i = 0; i < geom.getNumGeometries(); ++i) {
				mvtGeom.addAll(linesToGeomCmds(geom.getGeometryN(i), mvtClosePath, cursor, 1));
			}
		} else if(geom instanceof MultiPolygon || geom instanceof Polygon) {
			for(int i = 0; i < geom.getNumGeometries(); ++i) {
				final Polygon nextPoly = (Polygon) geom.getGeometryN(i);
				final List<Integer> nextPolyGeom = new ArrayList<>();
				boolean valid = true;
				final LineString exteriorRing = nextPoly.getExteriorRing();
				final double exteriorArea = Area.ofRingSigned(exteriorRing.getCoordinates());
				if(((int) Math.round(exteriorArea)) == 0) {
					continue;
				}
				if(exteriorArea > 0d) {
					CoordinateArrays.reverse(exteriorRing.getCoordinates());
				}
				nextPolyGeom.addAll(linesToGeomCmds(exteriorRing, mvtClosePath, cursor, 2));
				for(int ringIndex = 0; ringIndex < nextPoly.getNumInteriorRing(); ++ringIndex) {
					final LineString nextInteriorRing = nextPoly.getInteriorRingN(ringIndex);
					final double interiorArea = Area.ofRingSigned(nextInteriorRing.getCoordinates());
					if(((int)Math.round(interiorArea)) == 0) {
						continue;
					}
					if(interiorArea < 0d) {
						CoordinateArrays.reverse(nextInteriorRing.getCoordinates());
					}
					if(Math.abs(exteriorArea) <= Math.abs(interiorArea)) {
						valid = false;
						break;
					}
					nextPolyGeom.addAll(linesToGeomCmds(nextInteriorRing, mvtClosePath, cursor, 2));
				}
				if(valid) {
					mvtGeom.addAll(nextPolyGeom);
				}
			}
		}
		if(mvtGeom.size() < 1) {
			return null;
		}
		featureBuilder.addAllGeometry(mvtGeom);
		userDataConverter.addTags(geom.getUserData(), layerProps, featureBuilder);
		return featureBuilder.build();
	}

	private static List<Integer> ptsToGeomCmds(final Geometry geom, final Vec2d cursor) {
		final Coordinate[] geomCoords = geom.getCoordinates();
		if(geomCoords.length <= 0) {
			return Collections.emptyList();
		}
		final List<Integer> geomCmds = new ArrayList<>(geomCmdBuffLenPts(geomCoords.length));
		final Vec2d mvtPos = new Vec2d();
		int moveCmdLen = 0;
		geomCmds.add(0);
		Coordinate nextCoord;
//		for(int i = 0; i < geomCoords.length; ++i) {
		for(int i = geomCoords.length-1; i >=0; --i) {
			nextCoord = geomCoords[i];
			mvtPos.set(nextCoord.x, nextCoord.y);
//			if(i == 0 || !equalAsInts(cursor, mvtPos)) {
			if(i == geomCoords.length-1 || !equalAsInts(cursor, mvtPos)) {
				++moveCmdLen;
				moveCursor(cursor, geomCmds, mvtPos);
			}
		}
		if(moveCmdLen <= GeomCmdHdr.CMD_HDR_LEN_MAX) {
			geomCmds.set(0, GeomCmdHdr.cmdHdr(GeomCmd.MoveTo, moveCmdLen));
			return geomCmds;
		} else {
			return Collections.emptyList();
		}
	}

	private static List<Integer> linesToGeomCmds(
			final Geometry geom,
			final boolean closeEnabled,
			final Vec2d cursor,
			final int minLineToLen) {

		final Coordinate[] geomCoords = geom.getCoordinates();
		final int geomProcCoordCount;
		if(closeEnabled) {
			final int repeatEndCoordCount = countCoordRepeatReverse(geomCoords);
			geomProcCoordCount = geomCoords.length - repeatEndCoordCount;
		} else {
			geomProcCoordCount = geomCoords.length;
		}
		if(geomProcCoordCount < 2) {
			return Collections.emptyList();
		}
		final Vec2d origCursorPos = new Vec2d(cursor);
		final List<Integer> geomCmds = new ArrayList<>(geomCmdBuffLenLines(geomProcCoordCount, closeEnabled));
		final Vec2d mvtPos = new Vec2d();
		Coordinate nextCoord = geomCoords[0];
		mvtPos.set(nextCoord.x, nextCoord.y);
		geomCmds.add(GeomCmdHdr.cmdHdr(GeomCmd.MoveTo, 1));
		moveCursor(cursor, geomCmds, mvtPos);
		final int lineToCmdHdrIndex = geomCmds.size();
		geomCmds.add(0);
		int lineToLength = 0;
		for(int i = 1; i < geomProcCoordCount; ++i) {
			nextCoord = geomCoords[i];
			mvtPos.set(nextCoord.x, nextCoord.y);
			if(!equalAsInts(cursor, mvtPos)) {
				++lineToLength;
				moveCursor(cursor, geomCmds, mvtPos);
			}
		}
		if(lineToLength >= minLineToLen && lineToLength <= GeomCmdHdr.CMD_HDR_LEN_MAX) {
			geomCmds.set(lineToCmdHdrIndex, GeomCmdHdr.cmdHdr(GeomCmd.LineTo, lineToLength));
			if(closeEnabled) {
				geomCmds.add(GeomCmdHdr.closePathCmdHdr());
			}
			return geomCmds;
		} else {
			cursor.set(origCursorPos);
			return Collections.emptyList();
		}
	}

	private static int countCoordRepeatReverse(Coordinate[] coords) {
		int repeatCoords = 0;
		final Coordinate firstCoord = coords[0];
		Coordinate nextCoord;
		for(int i = coords.length - 1; i > 0; --i) {
			nextCoord = coords[i];
			if(equalAsInts2d(firstCoord, nextCoord)) {
				++repeatCoords;
			} else {
				break;
			}
		}
		return repeatCoords;
	}

	private static void moveCursor(Vec2d cursor, List<Integer> geomCmds, Vec2d mvtPos) {
		geomCmds.add(ZigZag.encode((int)mvtPos.x - (int)cursor.x));
		geomCmds.add(ZigZag.encode((int)mvtPos.y - (int)cursor.y));
		cursor.set(mvtPos);
	}

	private static boolean equalAsInts2d(Coordinate a, Coordinate b) {
		return ((int)a.getOrdinate(0)) == ((int)b.getOrdinate(0))
				&& ((int)a.getOrdinate(1)) == ((int)b.getOrdinate(1));
	}

	private static boolean equalAsInts(Vec2d a, Vec2d b) {
		return ((int) a.x) == ((int) b.x) && ((int) a.y) == ((int) b.y);
	}

	private static int geomCmdBuffLenPts(int coordCount) {
		return 1 + (coordCount * 2);
	}

	private static int geomCmdBuffLenLines(int coordCount, boolean closeEnabled) {
		return 2 + (closeEnabled ? 1 : 0) + (coordCount * 2);
	}
}
