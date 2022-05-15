package net.termat.geo.lod2mvt;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.wdtinc.mapbox_vector_tile.VectorTile;
import com.wdtinc.mapbox_vector_tile.VectorTile.Tile;
import com.wdtinc.mapbox_vector_tile.VectorTile.Tile.Feature.Builder;
import com.wdtinc.mapbox_vector_tile.adapt.jts.IGeometryFilter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.IUserDataConverter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.JtsAdapter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.MvtReader;
import com.wdtinc.mapbox_vector_tile.adapt.jts.TagKeyValueMapConverter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.TileGeomResult;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsLayer;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsMvt;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerBuild;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerParams;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerProps;

public class MVTBuilder {
	private List<Geometry> geom;
	private Rectangle2D bounds;
	private String layerName;
	
	public MVTBuilder(List<Geometry> geom,Rectangle2D bounds,String layerName) throws ParseException {
		this.bounds=bounds;
		this.geom=geom;
		this.layerName=layerName;
	}
		
	private static JsonObject createObj(Map<String,Object> map) {
		JsonObject ret=new JsonObject();
		for(String key  : map.keySet()) {
			Object o=map.get(key);
			if(o instanceof Number) {
				ret.addProperty(key, (Number)o);
			}else if(o instanceof String) {
				ret.addProperty(key, (String)o);
			}else if(o instanceof Character) {
				ret.addProperty(key, (Character)o);
			}else if(o instanceof Boolean) {
				ret.addProperty(key, (Boolean)o);
			}
		}
		return ret;
	}
	
	public void createMVT(int zoom,File dir) throws IOException {
		GeometryFactory geomFactory = new GeometryFactory();
		List<Point> list=TileMeshUtil.getTileList(this.bounds, zoom);
		File out=new File(dir.getAbsolutePath()+"\\"+Integer.toString(zoom));
		if(!out.exists())out.mkdir();
		for(Point p : list) {
			File out2=new File(out.getAbsolutePath()+"\\"+Integer.toString(p.x));
			if(!out2.exists())out2.mkdir();
			File out3=new File(out2.getAbsolutePath()+"\\"+Integer.toString(p.y)+".pbf");
			JtsMvt jtsmvt=null;
			if(out3.exists()) {
				jtsmvt=getMvtLayer(out3);
			}
			MvtLayerParams layerParams = new MvtLayerParams();
			Envelope env=getTileBounds(p.x,p.y,zoom);
			IGeometryFilter acceptAllGeomFilter=new IGeometryFilter() {
				@Override
				public boolean accept(Geometry geometry) {
					return true;
				}
			};
			TileGeomResult tileGeom = JtsAdapter.createTileGeom(geom, env, geomFactory, layerParams, acceptAllGeomFilter);
			final VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();
			final MvtLayerProps layerProps = new MvtLayerProps();
			IUserDataConverter userDataConverter=new IUserDataConverter() {
				@SuppressWarnings("unchecked")
				@Override
				public void addTags(Object userData, MvtLayerProps layerProps, Builder featureBuilder) {
					JsonObject o=null;
					if(userData instanceof Map) {
						o=createObj((Map<String,Object>)userData);
					}else {
						o=(JsonObject)userData;
					}
					for(String s : o.keySet()) {
						if(layerProps.keyIndex(layerName)==null) {
							 featureBuilder.addTags(layerProps.addKey(s));
						}
						String val=o.get(s).getAsString();
						try {
							 featureBuilder.addTags(layerProps.addValue(Double.parseDouble(val)));
						}catch(Exception e) {
							 featureBuilder.addTags(layerProps.addValue(val));
						}
					}
				}
			};
			if(tileGeom.mvtGeoms.size()==0)continue;
			if(jtsmvt==null) {
				final List<VectorTile.Tile.Feature> features = JtsAdapterReverse.toFeatures(tileGeom.mvtGeoms, layerProps, userDataConverter);
				final VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(layerName, layerParams);
				layerBuilder.addAllFeatures(features);
				MvtLayerBuild.writeProps(layerBuilder, layerProps);
				final VectorTile.Tile.Layer layer = layerBuilder.build();
				tileBuilder.addLayers(layer);
				Tile mvt = tileBuilder.build();
				byte[] bytes=mvt.toByteArray();
		        Path path = out3.toPath();
		        Files.write(path, bytes);
			}else {
				Map<String,JtsLayer> ll=jtsmvt.getLayersByName();
				for(String key : ll.keySet()) {
					JtsLayer lay=ll.get(key);
					if(key.equals(layerName)) {	
						List<Geometry> gg=new ArrayList<>();
						gg.addAll(lay.getGeometries());
						gg.addAll(tileGeom.mvtGeoms);
						final List<VectorTile.Tile.Feature> features = JtsAdapterReverse.toFeatures(gg, layerProps, userDataConverter);
						final VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(key, layerParams);
						layerBuilder.addAllFeatures(features);		
						MvtLayerBuild.writeProps(layerBuilder, layerProps);
						final VectorTile.Tile.Layer layer = layerBuilder.build();
						tileBuilder.addLayers(layer);
					}else {
						final List<VectorTile.Tile.Feature> features = JtsAdapterReverse.toFeatures(lay.getGeometries(), layerProps, userDataConverter);
						final VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(key, layerParams);
						layerBuilder.addAllFeatures(features);		
						MvtLayerBuild.writeProps(layerBuilder, layerProps);
						final VectorTile.Tile.Layer layer = layerBuilder.build();
						tileBuilder.addLayers(layer);
					}
				}
				if(!ll.keySet().contains(layerName)){
					final List<VectorTile.Tile.Feature> features = JtsAdapterReverse.toFeatures(tileGeom.mvtGeoms, layerProps, userDataConverter);
					final VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(layerName, layerParams);
					layerBuilder.addAllFeatures(features);
					MvtLayerBuild.writeProps(layerBuilder, layerProps);
					final VectorTile.Tile.Layer layer = layerBuilder.build();
					tileBuilder.addLayers(layer);
				}
				Tile mvt = tileBuilder.build();
				byte[] bytes=mvt.toByteArray();
		        Path path = out3.toPath();
		        Files.write(path, bytes);
			}
		}
	}
	
	public void createMVTs(int minZoom,int maxZoom,File out) {
		for(int i=minZoom;i<=maxZoom;i++) {
			try {
				this.createMVT(i, out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static Envelope getTileBounds(int x, int y, int zoom){
	    return new Envelope(getLong(x, zoom), getLong(x + 1, zoom), getLat(y, zoom), getLat(y + 1, zoom));
	}

	public static double getLong(int x, int zoom){
	    return ( x / Math.pow(2, zoom) * 360 - 180 );
	}

	public static double getLat(int y, int zoom){
	    double r2d = 180 / Math.PI;
	    double n = Math.PI - 2 * Math.PI * y / Math.pow(2, zoom);
	    return r2d * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n)));
	}
	
	public static JtsMvt getMvtLayer(File f) throws IOException{
		ByteArrayInputStream bis=new ByteArrayInputStream(getProto(f));
		GeometryFactory geomFactory = new GeometryFactory();
		JtsMvt mvt = MvtReader.loadMvt(bis,geomFactory,new TagKeyValueMapConverter(),MvtReader.RING_CLASSIFIER_V1);
		return mvt;
	}
		
	private static byte[] getProto(File f) throws IOException {
		ByteString bs=ByteString.readFrom(new FileInputStream(f));
		return bs.toByteArray();
	}
}
