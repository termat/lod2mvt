package net.termat.geo.lod2mvt;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class TileMeshUtil {
	public static int NA=new Color(128,0,0).getRGB();
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

	/**
	 * RGB（int)を標高に変換するメソッド
	 *
	 * @param  color RGB:int
	 * @return 標高:double
	 */
	public static float getZ(int color){
		color=(color << 8) >> 8;
		if(color==8388608||color==-8388608){
			return Float.NaN;
		}else if(color<8388608){
			return color * 0.01f;
		}else{
			return (color-16777216)*0.01f;
		}
	}

	/**
	 * 標高をRGB（INT）に変換するメソッド
	 *
	 * @param  z 標高 :double
	 * @return RGB:int
	 */
	public static int getRGB(float z){
		if(Float.isNaN(z)){
			return NA;
		}else if(z<-83886||z>83886) {
			return NA;
		}else{
			int i=(int)Math.round(z*100);
			if(z<0)	i=i+0x1000000;
			int r=Math.max(0,Math.min(i >> 16,255));
			int g=Math.max(0,Math.min(i-(r << 16) >> 8,255));
			int b=Math.max(0,Math.min(i-((r << 16)+(g << 8)),255));
			return new Color(r,g,b).getRGB();
		}
	}
	
	public static float[][] getDM(BufferedImage dem){
		int w=dem.getWidth();
		int h=dem.getHeight();
		float[][] ret=new float[w][h];
		for(int i=0;i<w;i++) {
			for(int j=0;j<h;j++) {
				ret[i][j]=getZ(dem.getRGB(i, j));
			}
		}
		return ret;
	}
}
