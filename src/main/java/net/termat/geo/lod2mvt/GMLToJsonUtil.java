package net.termat.geo.lod2mvt;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.citygml4j.CityGMLContext;
import org.citygml4j.builder.jaxb.CityGMLBuilder;
import org.citygml4j.builder.jaxb.CityGMLBuilderException;
import org.citygml4j.model.citygml.CityGML;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.bridge.Bridge;
import org.citygml4j.model.citygml.building.AbstractBoundarySurface;
import org.citygml4j.model.citygml.building.BoundarySurfaceProperty;
import org.citygml4j.model.citygml.building.Building;
import org.citygml4j.model.citygml.building.OuterFloorSurface;
import org.citygml4j.model.citygml.building.RoofSurface;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.citygml.core.CityModel;
import org.citygml4j.model.citygml.core.CityObjectMember;
import org.citygml4j.model.citygml.generics.AbstractGenericAttribute;
import org.citygml4j.model.citygml.generics.IntAttribute;
import org.citygml4j.model.citygml.generics.MeasureAttribute;
import org.citygml4j.model.citygml.generics.StringAttribute;
import org.citygml4j.model.citygml.waterbody.WaterBody;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurface;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;
import org.citygml4j.model.gml.geometry.primitives.AbstractRingProperty;
import org.citygml4j.model.gml.geometry.primitives.DirectPositionList;
import org.citygml4j.model.gml.geometry.primitives.Exterior;
import org.citygml4j.model.gml.geometry.primitives.LinearRing;
import org.citygml4j.model.gml.geometry.primitives.Polygon;
import org.citygml4j.model.gml.geometry.primitives.SurfaceProperty;
import org.citygml4j.xml.io.CityGMLInputFactory;
import org.citygml4j.xml.io.reader.CityGMLReadException;
import org.citygml4j.xml.io.reader.CityGMLReader;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;

import com.google.gson.JsonObject;

public class GMLToJsonUtil {

	/**
	 * 
	 * @param f		CityGNLのファイル
	 * @param attr	属性を設定するか否か
	 * @return		Geometryのリスト(UserDataにJsonObjectを保持)
	 * @throws CityGMLBuilderException
	 * @throws CityGMLReadException
	 * @throws ParseException
	 */
	public static Map<String,List<Geometry>> gmlToJsonBldg(File f,boolean attr) throws CityGMLBuilderException, CityGMLReadException, ParseException{
		Map<String,List<Geometry>> ret=new HashMap<>();
		CityGMLContext ctx = CityGMLContext.getInstance();
		CityGMLBuilder builder = ctx.createCityGMLBuilder();
		CityGMLInputFactory in = builder.createCityGMLInputFactory();
		CityGMLReader reader = in.createCityGMLReader(f);
		while (reader.hasNext()) {
			CityGML citygml = reader.nextFeature();
			if (citygml.getCityGMLClass() == CityGMLClass.CITY_MODEL) {
				CityModel cityModel = (CityModel)citygml;
				for (CityObjectMember cityObjectMember : cityModel.getCityObjectMember()) {
					AbstractCityObject cityObject = cityObjectMember.getCityObject();
					String layer=cityObject.getCityGMLClass().name();
					if (cityObject.getCityGMLClass() == CityGMLClass.BUILDING){
						List<Geometry> geom=null;
						if(ret.containsKey(layer)) {
							geom=ret.get(layer);
						}else {
							geom=new ArrayList<>();
							ret.put(layer,geom);
						}
						Building b=(Building)cityObject;
						if(b.getMeasuredHeight()!=null){
							createBldg(b,geom,attr);
						}
					}else if(cityObject.getCityGMLClass() == CityGMLClass.BRIDGE) {
						List<Geometry> geom=null;
						if(ret.containsKey(layer)) {
							geom=ret.get(layer);
						}else {
							geom=new ArrayList<>();
							ret.put(layer,geom);
						}
						Bridge b=(Bridge)cityObject;
						createBridge(b,geom,attr);
					}else if(cityObject.getCityGMLClass() == CityGMLClass.WATER_BODY) {
						List<Geometry> geom=null;
						if(ret.containsKey(layer)) {
							geom=ret.get(layer);
						}else {
							geom=new ArrayList<>();
							ret.put(layer,geom);
						}
						WaterBody b=(WaterBody)cityObject;
						createWaterBody(b,geom,attr);
					}
				}
			}
		}
		reader.close();
		return ret;
	}
	
	private static double getDem(MultiSurfaceProperty msp) {
		MultiSurface ms=msp.getMultiSurface();
		List<SurfaceProperty> spl=ms.getSurfaceMember();
		SurfaceProperty ps=spl.get(0);
		Polygon pl=(Polygon)ps.getGeometry();		
		Exterior ex=(Exterior)pl.getExterior();
		LinearRing lr=(LinearRing)ex.getRing();
		DirectPositionList dpl=(DirectPositionList)lr.getPosList();
		List<Double> dl=dpl.toList3d();
		double dem=10000;
		for(int i=0;i<dl.size();i=i+3){
			Double d03=dl.get(i+2);
			dem=Math.min(dem, d03);
		}
		return dem;
	}
	
	/*
	 * 図形のExteriorを取得
	 */
	private static org.locationtech.jts.geom.LinearRing getExterior(Polygon pp,GeometryFactory gf,double dem) {
		Exterior ex=(Exterior)pp.getExterior();
		LinearRing lr=(LinearRing)ex.getRing();
		DirectPositionList dpl=(DirectPositionList)lr.getPosList();
		List<Double> dl=dpl.toList3d();
		List<Coordinate> tmp=new ArrayList<>();
		for(int i=0;i<dl.size();i=i+3){
			Double d01=dl.get(i);
			Double d02=dl.get(i+1);
			Double d03=dl.get(i+2);
			tmp.add(new Coordinate(d02,d01,d03-dem));
		}
		return gf.createLinearRing(tmp.toArray(new Coordinate[tmp.size()]));
	}

	private static org.locationtech.jts.geom.LinearRing[] getInerior(Polygon pp,GeometryFactory gf,double dem) {
		List<AbstractRingProperty> ll=pp.getInterior();
		List<org.locationtech.jts.geom.LinearRing> ret=new ArrayList<>();
		for(AbstractRingProperty ex : ll) {
			LinearRing lr=(LinearRing)ex.getRing();
			DirectPositionList dpl=(DirectPositionList)lr.getPosList();
			List<Double> dl=dpl.toList3d();
			List<Coordinate> tmp=new ArrayList<>();
			for(int i=0;i<dl.size();i=i+3){
				Double d01=dl.get(i);
				Double d02=dl.get(i+1);
				Double d03=dl.get(i+2);
				tmp.add(new Coordinate(d02,d01,d03-dem));
			}
			ret.add(gf.createLinearRing(tmp.toArray(new Coordinate[tmp.size()])));
		}
		return ret.toArray(new org.locationtech.jts.geom.LinearRing[ret.size()]);
	}
	
	private static JsonObject getAttributes(List<AbstractGenericAttribute> list) {
		JsonObject prop=new JsonObject();
        for(AbstractGenericAttribute at : list){
            if(at instanceof StringAttribute){
                StringAttribute st=(StringAttribute)at;
                prop.addProperty(st.getName(), st.getValue());
            }else if(at instanceof MeasureAttribute){
                MeasureAttribute st=(MeasureAttribute)at;
                prop.addProperty(st.getName(), st.getValue().getValue());
            }else if(at instanceof IntAttribute){
                IntAttribute st=(IntAttribute)at;
                prop.addProperty(st.getName(), st.getValue());
            }
        }
		return prop;
	}

	private static List<org.locationtech.jts.geom.Polygon> createGeometry(MultiSurfaceProperty msp,double dem) {
		GeometryFactory gf=new GeometryFactory();
		MultiSurface ms=msp.getMultiSurface();
		List<SurfaceProperty> spl=ms.getSurfaceMember();
		List<org.locationtech.jts.geom.Polygon> ret=new ArrayList<>();
		for(SurfaceProperty s : spl) {
			org.locationtech.jts.geom.LinearRing lr01=getExterior((Polygon)s.getGeometry(),gf,dem);
			org.locationtech.jts.geom.LinearRing[] lr02=getInerior((Polygon)s.getGeometry(),gf,dem);
			org.locationtech.jts.geom.Polygon poly=null;
			if(lr02.length==0) {
				poly=gf.createPolygon(lr01);
			}else {
				poly=gf.createPolygon(lr01,lr02);
			}
			ret.add(poly);
		}
		return ret;
	}
	
	private static List<org.locationtech.jts.geom.Polygon> createGeometry(List<SurfaceProperty> spl,double dem) {
		GeometryFactory gf=new GeometryFactory();
		List<org.locationtech.jts.geom.Polygon> ret=new ArrayList<>();
		for(SurfaceProperty s : spl) {
			org.locationtech.jts.geom.LinearRing lr01=getExterior((Polygon)s.getGeometry(),gf,dem);
			org.locationtech.jts.geom.LinearRing[] lr02=getInerior((Polygon)s.getGeometry(),gf,dem);
			org.locationtech.jts.geom.Polygon poly=null;
			if(lr02.length==0) {
				poly=gf.createPolygon(lr01);
			}else {
				poly=gf.createPolygon(lr01,lr02);
			}
			ret.add(poly);
		}
		return ret;
	}
	
	private static MultiSurfaceProperty getLOD0MultiSurfaceProperty(Building b) {
		MultiSurfaceProperty msp=b.getLod0FootPrint();
		if(msp!=null) {
			return msp;
		}else {
			return b.getLod0RoofEdge();
		}
	}
	
    private static double getLowestDem(List<BoundarySurfaceProperty> bs) {
    	double ret=1000000;
    	for(BoundarySurfaceProperty st : bs) {
    		AbstractBoundarySurface as=st.getBoundarySurface();
    		if(as instanceof RoofSurface) {
    			RoofSurface rs=(RoofSurface)as;
    			MultiSurfaceProperty msp=rs.getLod2MultiSurface();
    			MultiSurface ms=msp.getMultiSurface();
    			List<SurfaceProperty> spl=ms.getSurfaceMember();
    			for(SurfaceProperty s :spl) {
    				Polygon pp=(Polygon)s.getGeometry();
    				Exterior ex=(Exterior)pp.getExterior();
    				LinearRing lr=(LinearRing)ex.getRing();
    				DirectPositionList dpl=(DirectPositionList)lr.getPosList();
    				List<Double> dl=dpl.toList3d();
    				for(int i=0;i<dl.size();i=i+3){
    					Double d03=dl.get(i+2);
    					ret=Math.min(ret, d03);
    				}
    			}

    		}else if(as instanceof OuterFloorSurface) {
    			OuterFloorSurface rs=(OuterFloorSurface)as;
    			MultiSurfaceProperty msp=rs.getLod2MultiSurface();
    			MultiSurface ms=msp.getMultiSurface();
    			List<SurfaceProperty> spl=ms.getSurfaceMember();
    			for(SurfaceProperty s :spl) {
    				Polygon pp=(Polygon)s.getGeometry();
    				Exterior ex=(Exterior)pp.getExterior();
    				LinearRing lr=(LinearRing)ex.getRing();
    				DirectPositionList dpl=(DirectPositionList)lr.getPosList();
    				List<Double> dl=dpl.toList3d();
    				for(int i=0;i<dl.size();i=i+3){
    					Double d03=dl.get(i+2);
    					ret=Math.min(ret, d03);
    				}
    			}
    		}
    	}
    	return ret;
    }
    
    private static double getLowest(List<SurfaceProperty> bs) {
    	double ret=1000000;
    	for(SurfaceProperty st : bs) {
			Polygon pp=(Polygon)st.getGeometry();
			Exterior ex=(Exterior)pp.getExterior();
			LinearRing lr=(LinearRing)ex.getRing();
			DirectPositionList dpl=(DirectPositionList)lr.getPosList();
			List<Double> dl=dpl.toList3d();
			for(int i=0;i<dl.size();i=i+3){
				Double d03=dl.get(i+2);
				ret=Math.min(ret, d03);
			}
    	}
    	return ret;
    }
	private static double getVal(LineString ls) {
		double ave=0.0;
		Coordinate[] c=ls.getCoordinates();
		if(c.length==0)return 0.0;
		for(Coordinate p : c) {
			ave +=p.getZ();
		}
		return ave/c.length;
	}


	private static void createBldg(Building b,List<Geometry> geom,boolean attr){
		List<BoundarySurfaceProperty> bs=b.getBoundedBySurface();
		if(bs.size()==0) {
			createBldgLOD0(b,geom,attr);
		}else {
			createBldgLOD2(b,geom,attr);
		}
	}
	
	private static void createBldgLOD0(Building b,List<Geometry> geom,boolean attr){
		MultiSurfaceProperty msp=getLOD0MultiSurfaceProperty(b);
		List<org.locationtech.jts.geom.Polygon> ll=createGeometry(msp,0);
    	JsonObject prop=null;
    	if(attr)prop=getAttributes(b.getGenericAttribute());
		for(org.locationtech.jts.geom.Polygon pl : ll) {
			JsonObject obj=null;
			if(attr) {
				obj=prop.deepCopy();
			}else {
				obj=new JsonObject();
			}
			obj.addProperty("height", b.getMeasuredHeight().getValue());
			pl.setUserData(obj);
			geom.add(pl);
		}
	}

    private static void createBldgLOD2(Building b,List<Geometry> geom,boolean attr){
    	MultiSurfaceProperty mm=b.getLod0FootPrint();
    	List<BoundarySurfaceProperty> bs=b.getBoundedBySurface();
    	double dem=0.0;
    	if(mm!=null) {
    		dem=getDem(mm);
    	}else {
    		dem=getLowestDem(bs);
    	}
    	JsonObject prop=null;
    	if(attr)prop=getAttributes(b.getGenericAttribute());
    	for(BoundarySurfaceProperty st : bs) {
    		AbstractBoundarySurface as=st.getBoundarySurface();
    		if(as instanceof RoofSurface) {
    			RoofSurface rs=(RoofSurface)as;
    			MultiSurfaceProperty msp=rs.getLod2MultiSurface();
    			List<org.locationtech.jts.geom.Polygon> ll=createGeometry(msp,dem);
    			for(org.locationtech.jts.geom.Polygon pl : ll) {
    				JsonObject obj=null;
    				if(attr) {
    					obj=prop.deepCopy();
    				}else {
    					obj=new JsonObject();
    				}
        	        obj.addProperty("height", getVal(pl.getExteriorRing()));
        	        pl.setUserData(obj);
        	        geom.add(pl);
    			}
    		}else if(as instanceof OuterFloorSurface) {
    			OuterFloorSurface rs=(OuterFloorSurface)as;
    			MultiSurfaceProperty msp=rs.getLod2MultiSurface();
    			List<org.locationtech.jts.geom.Polygon> ll=createGeometry(msp,dem);
    			for(org.locationtech.jts.geom.Polygon pl : ll) {
    				JsonObject obj=null;
    				if(attr) {
    					obj=prop.deepCopy();
    				}else {
    					obj=new JsonObject();
    				}
        	        obj.addProperty("height", getVal(pl.getExteriorRing()));
        	        pl.setUserData(obj);
        	        geom.add(pl);
    			}
    		}
    	}
    }
    
    private static void createBridge(Bridge b,List<Geometry> geom,boolean attr){
    	MultiSurfaceProperty mm=b.getLod2MultiSurface();
    	if(mm==null)mm=b.getLod1MultiSurface();
    	if(mm==null)return;
    	MultiSurface ms=mm.getMultiSurface();
    	List<SurfaceProperty> spl=ms.getSurfaceMember();
    	double low=getLowest(spl);
    	List<org.locationtech.jts.geom.Polygon> ll=createGeometry(spl,low);
    	JsonObject prop=null;
    	if(attr)prop=getAttributes(b.getGenericAttribute());
		for(org.locationtech.jts.geom.Polygon pl : ll) {
			JsonObject obj=null;
			if(attr) {
				obj=prop.deepCopy();
			}else {
				obj=new JsonObject();
			}
	        obj.addProperty("height", getVal(pl.getExteriorRing()));
	        pl.setUserData(obj);
	        geom.add(pl);
		}
    }
    
    private static void createWaterBody(WaterBody b,List<Geometry> geom,boolean attr){
    	MultiSurfaceProperty mm=b.getLod1MultiSurface();
    	MultiSurface ms=mm.getMultiSurface();
    	List<SurfaceProperty> spl=ms.getSurfaceMember();
    	double low=getLowest(spl);
    	List<org.locationtech.jts.geom.Polygon> ll=createGeometry(spl,low);
    	JsonObject prop=null;
    	if(attr)prop=getAttributes(b.getGenericAttribute());
		for(org.locationtech.jts.geom.Polygon pl : ll) {
			JsonObject obj=null;
			if(attr) {
				obj=prop.deepCopy();
			}else {
				obj=new JsonObject();
			}
	        obj.addProperty("height", getVal(pl.getExteriorRing()));
	        pl.setUserData(obj);
	        geom.add(pl);
		}
    }
	
	/**
	 * 全てのgeometryを含む矩形領域を取得
	 * 
	 * @param geom
	 * @return
	 */
	public static Rectangle2D getBounds(List<Geometry> geom) {
		Rectangle2D ret=null;
		for(Geometry g : geom) {
			Coordinate[] cc=g.getCoordinates();
			for(Coordinate c : cc) {
				if(ret==null) {
					ret=new Rectangle2D.Double(c.getX(),c.getY(),0,0);
				}else {
					ret.add(c.getX(), c.getY());
				}
				
			}
		}
		return ret;
	}
}
