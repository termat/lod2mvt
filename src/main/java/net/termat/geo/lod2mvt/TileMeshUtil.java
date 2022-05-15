package net.termat.geo.lod2mvt;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class TileMeshUtil {
	private static final double L=85.05112877980659;

	/**
	 * 緯度経度からピクセル座標を取得
	 * 
	 * @param zoom ズームレベル
	 * @param lon 経度
	 * @param lat 緯度
	 * @return
	 */
	public static Point2D lonlatToPixel(int zoom,double lon,double lat){
		double x=(Math.pow(2, zoom+7)*(lon/180.0+1.0));
		double y=((Math.pow(2, zoom+7)/Math.PI)*(-atanh(Math.sin(Math.toRadians(lat)))+atanh(Math.sin(Math.toRadians(L)))));
		return new Point2D.Double(x,y);
	}
	
	/**
	 * 緯度経度からタイル座標を取得
	 * 
	 * @param zoom ズームレベル
	 * @param lon 経度
	 * @param lat 緯度
	 * @return
	 */
	public static Point lonlatToTile(int zoom,double lon,double lat){
		Point2D pixel=lonlatToPixel(zoom,lon,lat);
		Point tile=new Point((int)Math.floor(pixel.getX()/256.0),(int)Math.floor(pixel.getY()/256.0));
		return tile;
	}
	
	/**
	 * 領域と交差するタイル座標リストを取得
	 * 
	 * @param rect 領域
	 * @param zoom ズームレベル
	 * @return
	 */
	public static List<Point> getTileList(Rectangle2D rect,int zoom){
		Point p1=lonlatToTile(zoom,rect.getX(),rect.getY());
		Point p2=lonlatToTile(zoom,rect.getX()+rect.getWidth(),rect.getY()+rect.getHeight());
		int xmin=(int)Math.min(p1.getX(), p2.getX());
		int xmax=(int)Math.max(p1.getX(), p2.getX());
		int ymin=(int)Math.min(p1.getY(), p2.getY());
		int ymax=(int)Math.max(p1.getY(), p2.getY());
		List<Point> ret=new ArrayList<>();
		for(int i=xmin;i<=xmax;i++) {
			for(int j=ymin;j<=ymax;j++) {
				ret.add(new Point(i,j));
			}
		}
		return ret;
	}
	
	/**
	 * タイル座標の緯度経度領域を取得
	 * 
	 * @param zoom ズームレベル
	 * @param tileX タイル座標x
	 * @param tileY タイル座標y
	 * @return
	 */
	public static Rectangle2D getTileBounds(int zoom,int tileX,int tileY) {
		double px=tileX*256;
		double py=tileY*256;
		double lon1=180*(px/Math.pow(2, zoom+7)-1);
		double lat1=(180.0/Math.PI)*Math.asin(Math.tanh(-(Math.PI/Math.pow(2,zoom+7))*py+atanh(Math.sin(Math.PI/180*L))));
		double lon2=180*((px+256)/Math.pow(2, zoom+7)-1);
		double lat2=(180.0/Math.PI)*Math.asin(Math.tanh(-(Math.PI/Math.pow(2,zoom+7))*(py+256)+atanh(Math.sin(Math.PI/180*L))));
		double xmin=Math.min(lon1,lon2);
		double xmax=Math.max(lon1, lon2);
		double ymin=Math.min(lat1,lat2);
		double ymax=Math.max(lat1, lat2);
		return new Rectangle2D.Double(xmin,ymin,xmax-xmin,ymax-ymin);
	}

	private static double atanh(double v){
		return 0.5*Math.log((1.0+v)/(1.0-v));
	}
}
