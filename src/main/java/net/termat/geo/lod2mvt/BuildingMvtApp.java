package net.termat.geo.lod2mvt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

import org.locationtech.jts.awt.ShapeWriter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.MultiPolygon;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.wdtinc.mapbox_vector_tile.VectorTile;
import com.wdtinc.mapbox_vector_tile.VectorTile.Tile;
import com.wdtinc.mapbox_vector_tile.VectorTile.Tile.Feature.Builder;
import com.wdtinc.mapbox_vector_tile.adapt.jts.IUserDataConverter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.MvtReader;
import com.wdtinc.mapbox_vector_tile.adapt.jts.TagKeyValueMapConverter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsLayer;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsMvt;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerBuild;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerParams;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerProps;

public class BuildingMvtApp {
	private JFrame frame;
	private JFileChooser ch;
	private DropJTextField dsm,dem,dchm,dst,file;
	private JComboBox<Integer> zoom01,zoom02;
	private JTextField check1,check2;
	private JButton exec;
	private Thread runner;
	private static ShapeWriter sw=new ShapeWriter();
	private static GeoJsonReader gr=new GeoJsonReader();
	private String vecurl="https://cyberjapandata.gsi.go.jp/xyz/experimental_bvmap/{z}/{x}/{y}.pbf";
	private String TEMP_FILE="temp.pbf";
	
	public BuildingMvtApp() {
		frame=new JFrame();
		frame.setTitle("BuildingMvt");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		if(!System.getProperty("os.name").toLowerCase().startsWith("mac")) {
			try {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
				SwingUtilities.updateComponentTreeUI(frame);
			}catch(Exception e){
				try {
					UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
					SwingUtilities.updateComponentTreeUI(frame);
				}catch(Exception ee){
					ee.printStackTrace();
				}
			}
		}
		WindowAdapter wa=new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		};
		frame.addWindowListener(wa);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.setResizable(false);
		frame.setSize(400, 600);
		frame.setLocationRelativeTo(null);
		ch=new JFileChooser();
		ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		JPanel pup=new JPanel(new GridLayout(8,1));
		frame.getContentPane().add(pup,BorderLayout.NORTH);
		JPanel p11=new JPanel(new BorderLayout());
		p11.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "■DSM"));
		dsm=new DropJTextField();
		dsm.setBorder(BorderFactory.createLoweredBevelBorder());
		dsm.setEditable(true);
		p11.add(dsm,BorderLayout.CENTER);		
		pup.add(p11);
		JPanel p12=new JPanel(new BorderLayout());
		p12.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "■DEM"));
		dem=new DropJTextField();
		dem.setBorder(BorderFactory.createLoweredBevelBorder());
		dem.setEditable(true);
		p12.add(dem,BorderLayout.CENTER);		
		pup.add(p12);
		JPanel p13=new JPanel(new BorderLayout());
		p13.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "■DCHM"));
		dchm=new DropJTextField();
		dchm.setBorder(BorderFactory.createLoweredBevelBorder());
		dchm.setEditable(true);
		p13.add(dchm,BorderLayout.CENTER);		
		pup.add(p13);
		
		dsm.setText("https://gio.pref.hyogo.lg.jp/tile/dsm/{z}/{y}/{x}.png");
		dem.setText("https://gio.pref.hyogo.lg.jp/tile/dem/{z}/{y}/{x}.png");
		
		JPanel p22=new JPanel(new BorderLayout());
		p22.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "■生成範囲（Geojson）"));
		file=new DropJTextField();
		file.setBorder(BorderFactory.createLoweredBevelBorder());
		file.setEditable(false);
		p22.add(file,BorderLayout.CENTER);
		pup.add(p22);
		JButton bt22=new JButton("－");
		bt22.addActionListener(e->{
			ch.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					if(f.isDirectory()) {
						return true;
					}else {
						return f.getName().toLowerCase().endsWith(".geojson");
					}
				}

				@Override
				public String getDescription() {
					return "*.geojson";
				}
			});
			ch.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			int ck=ch.showOpenDialog(frame);
			if(ck==JFileChooser.APPROVE_OPTION) {
				file.setText(ch.getSelectedFile().getAbsolutePath());
			}
		});
		p22.add(bt22,BorderLayout.EAST);
		
		
		JPanel p2=new JPanel(new BorderLayout());
		p2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "■出力先（MVT）"));
		dst=new DropJTextField();
		dst.setBorder(BorderFactory.createLoweredBevelBorder());
		dst.setEditable(false);
		p2.add(dst,BorderLayout.CENTER);
		pup.add(p2);
		JButton bt2=new JButton("－");
		bt2.addActionListener(e->{
			ch.setFileFilter(null);
			ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int ck=ch.showOpenDialog(frame);
			if(ck==JFileChooser.APPROVE_OPTION) {
				dst.setText(ch.getSelectedFile().getAbsolutePath());
			}
		});
		p2.add(bt2,BorderLayout.EAST);
		
		JPanel panel02=new JPanel(new GridLayout(1,2));
		pup.add(panel02);
		zoom01=new JComboBox<>(new Integer[] {10,11,12,13,14,15,16,17});
		zoom02=new JComboBox<>(new Integer[] {11,12,13,14,15,16,17,18});
		((JLabel)zoom01.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
		((JLabel)zoom02.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
		zoom01.addItemListener(e->{
			int i1=(Integer)zoom01.getSelectedItem();
			int i2=(Integer)zoom02.getSelectedItem();
			if(i1>=i2)zoom02.setSelectedIndex(zoom01.getSelectedIndex());
		});
		zoom02.addItemListener(e->{
			int i1=(Integer)zoom01.getSelectedItem();
			int i2=(Integer)zoom02.getSelectedItem();
			if(i1>=i2)zoom01.setSelectedIndex(zoom02.getSelectedIndex());
		});
		
		zoom01.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "最小ズームレベル"));
		panel02.add(zoom01);
		zoom02.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "最大ズームレベル"));
		panel02.add(zoom02);
	
		JPanel panel03=new JPanel(new GridLayout(1,2));
		pup.add(panel03);
		
		check1=new JTextField("building");
		JPanel tmp=new JPanel(new BorderLayout());
		tmp.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "入力レイヤー名"));
		tmp.add(check1,BorderLayout.CENTER);
		panel03.add(tmp);
		check2=new JTextField("BUILDING");
		JPanel tmp2=new JPanel(new BorderLayout());
		tmp2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "出力レイヤー名"));
		tmp2.add(check2,BorderLayout.CENTER);
		panel03.add(tmp2);
		
		exec=new JButton("処理開始");
		exec.addActionListener(e->{
			if(runner==null) {
				int ck=JOptionPane.showConfirmDialog(frame, "実行しますか？", "確認", JOptionPane.YES_NO_OPTION);
				if(ck==JOptionPane.YES_OPTION) {
					exec.setForeground(Color.RED);
					exec.setText("処理中止");
					Runnable r=new Runnable() {
						public void run() {
							try {
								exec(new File(file.getText()));
							} catch (ParseException | IOException e1) {
								e1.printStackTrace();
							}
							runner=null;
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									exec.setForeground(Color.BLACK);
									exec.setText("処理開始");
								}
							});
						}
					};
					runner=new Thread(r);
					runner.start();
				}
			}else {
				int ck=JOptionPane.showConfirmDialog(frame, "実行中の処理を中止しますか？", "確認", JOptionPane.YES_NO_OPTION);
				if(ck==JOptionPane.YES_OPTION) {
					if(!runner.isInterrupted()) {
						runner.interrupt();					
						runner=null;
						System.out.println("--- process stoped.---------------------------");
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								exec.setForeground(Color.BLACK);
								exec.setText("処理開始");
							}
						});
					}
				}
			}
		});
		pup.add(exec);
		JTextArea area=new JTextArea();
		area.setEditable(false);
		JTextAreaStream stream=new JTextAreaStream(area);
		System.setOut(new PrintStream(stream, true));
//		System.setErr(new PrintStream(stream, true));
		JPanel under=new JPanel(new BorderLayout());
		under.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "処理状況"));
		under.add(new JScrollPane(area),BorderLayout.CENTER);
		frame.getContentPane().add(under,BorderLayout.CENTER);
	}

	
	private void close(){
		int id=JOptionPane.showConfirmDialog(frame, "Exit?", "Info", JOptionPane.YES_NO_OPTION,JOptionPane.INFORMATION_MESSAGE);
		if(id==JOptionPane.YES_OPTION){
			frame.setVisible(false);
			System.exit(0);
		}
	}
	
	private List<java.awt.Point> getCoordList(File f,int zoom) throws IOException, ParseException{
		FeatureCollection fc=FeatureCollection.fromJson(readJson(f));
		Area area=null;
		for(Feature fe : fc.features()) {
			com.mapbox.geojson.Geometry mg=fe.geometry();
			if(mg instanceof MultiPolygon) {
				MultiPolygon mp=(MultiPolygon)mg;
				JsonObject jo=fe.properties();
				for(com.mapbox.geojson.Polygon cp : mp.polygons()) {
					Geometry g=toJTS(cp);
					g.setUserData(jo.deepCopy());
					if(area==null) {
						area=toShape(g);
					}else {
						area.add(toShape(g));
					}
				}
			}else {
				Geometry g=toJTS(mg);
				g.setUserData(fe.properties());
				if(area==null) {
					area=toShape(g);
				}else {
					area.add(toShape(g));
				}
			}
		}
		return TileMeshUtil.getTileList(area.getBounds2D(), zoom);
	}
	
	private void exec(File f) throws IOException, ParseException {
		if(!f.exists()) {
			return;
		}else if(!f.getName().toLowerCase().endsWith(".geojson")) {
			return;
		}
		int minZoom=(Integer)zoom01.getSelectedItem();
		int maxZoom=(Integer)zoom02.getSelectedItem();
		List<java.awt.Point> list=getCoordList(f,minZoom);
		List<java.awt.Point> list2=new ArrayList<>();

		String d1=dsm.getText();
		String d2=dem.getText();
		String d3=dchm.getText();
		GeometryFactory geomFactory = new GeometryFactory();
		
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
					if(layerProps.keyIndex(check2.getText())==null) {
						 featureBuilder.addTags(layerProps.addKey(s));
					}
					JsonElement je=o.get(s);
					try {
						String val=je.getAsString();
						try {
							featureBuilder.addTags(layerProps.addValue(Double.parseDouble(val)));
						}catch(NumberFormatException e) {
							featureBuilder.addTags(layerProps.addValue(val));
						}
					}catch(java.lang.UnsupportedOperationException ue) {
						System.out.println("NULL="+s);
					}
				}
			}
		};
		
		for(int i=minZoom;i<maxZoom;i++) {
			File out=new File(dst.getText()+"\\"+Integer.toString(i));
			if(!out.exists())out.mkdir();
			String u1=d1.replace("{z}", Integer.toString(i));
			String u2=d2.replace("{z}", Integer.toString(i));
			String u3=d3.replace("{z}", Integer.toString(i));
			for(java.awt.Point p : list) {
				System.out.println(i+" / "+p.x+" / "+p.y);
				try {
					if(d3.isBlank()||d3.isEmpty()||d3.length()==0) {
						URL url1=new URL(u1.replace("{x}", Integer.toString(p.x)).replace("{y}", Integer.toString(p.y)));
						URL url2=new URL(u2.replace("{x}", Integer.toString(p.x)).replace("{y}", Integer.toString(p.y)));
						BufferedImage img1=ImageIO.read(url1.openStream());
						BufferedImage img2=ImageIO.read(url2.openStream());
						float[][] dm=dchm(img1,img2);
						exec2(i,p.x,p.y,dm,geomFactory,userDataConverter,out);
					}else {
						URL url3=new URL(u3.replace("{x}", Integer.toString(p.x)).replace("{y}", Integer.toString(p.y)));
						BufferedImage img1=ImageIO.read(url3.openStream());
						float[][] dm=TileMeshUtil.getDM(img1);
						exec2(i,p.x,p.y,dm,geomFactory,userDataConverter,out);
					}
					list2.add(new java.awt.Point(p.x*2,p.y*2));
					list2.add(new java.awt.Point(p.x*2+1,p.y*2));
					list2.add(new java.awt.Point(p.x*2,p.y*2+1));
					list2.add(new java.awt.Point(p.x*2+1,p.y*2+1));
				}catch(java.io.FileNotFoundException e) {}
			}
			list.clear();
			list.addAll(list2);
			list2.clear();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void exec2(int zoom,int x,int y,float[][] dm,GeometryFactory geomFactory,IUserDataConverter userDataConverter,File out) throws IOException {
		download(zoom,x,y);
		JtsMvt jts = MvtReader.loadMvt(Paths.get(TEMP_FILE).toFile(),geomFactory,new TagKeyValueMapConverter(),MvtReader.RING_CLASSIFIER_V1);
		JtsLayer lay=jts.getLayer(check1.getText());
		if(lay==null)return;
		int ext=lay.getExtent();
		List<Geometry> geom=new ArrayList<>();
		for(Geometry g : lay.getGeometries()) {
			LinkedHashMap<String,Object> o=(LinkedHashMap<String,Object>)g.getUserData();
			float hh=getHeight(ext,dm,g);
			if(Float.isNaN(hh))continue;
			if(hh>0) {
				o.put("height", hh);
				geom.add(g);
			}
		}
		if(geom.size()==0)return;
		MvtLayerParams layerParams = new MvtLayerParams();
		final VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();
		final MvtLayerProps layerProps = new MvtLayerProps();
		final List<VectorTile.Tile.Feature> features = JtsAdapterReverse.toFeatures(geom, layerProps, userDataConverter);
		final VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(check2.getText(), layerParams);
		layerBuilder.addAllFeatures(features);
		MvtLayerBuild.writeProps(layerBuilder, layerProps);
		final VectorTile.Tile.Layer layer = layerBuilder.build();
		tileBuilder.addLayers(layer);
		Tile mvt = tileBuilder.build();
		byte[] bytes=mvt.toByteArray();
		File out2=new File(out.getAbsolutePath()+"\\"+Integer.toString(x));
		if(!out2.exists())out2.mkdir();
		File out3=new File(out2.getAbsolutePath()+"\\"+Integer.toString(y)+".pbf");
		Path path = out3.toPath();
        Files.write(path, bytes);
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
	
	private float getHeight(int ext,float[][] dm,Geometry g) {
		if(g.getArea()<25)return -1;
		Area aa=toShape(g);
		Rectangle2D r=aa.getBounds2D();	
		Point2D pt=new Point2D.Double(r.getCenterX(),r.getCenterY());
		while(!aa.contains(pt)) {
			double xx=r.getX()+r.getWidth()*Math.random();
			double yy=r.getY()+r.getHeight()*Math.random();
			pt=new Point2D.Double(xx,yy);
		}
		int px=(int)(pt.getX()/(double)ext*256.0);
		int py=(int)(pt.getY()/(double)ext*256.0);
		px=Math.max(Math.min(px,255),0);
		py=Math.max(Math.min(py,255),0);
		return dm[px][py];
	}
	
	private void download(int zoom,int x,int y) throws IOException {
		String u=vecurl.replace("{z}", Integer.toString(zoom)).replace("{x}", Integer.toString(x)).replace("{y}", Integer.toString(y));
		InputStream in = new URL(u).openStream();
		Files.copy(in, Paths.get(TEMP_FILE), StandardCopyOption.REPLACE_EXISTING);
	}
	
	class JTextAreaStream extends OutputStream {
		private JTextArea area;
		private ByteArrayOutputStream buf;
		private int maxLen=100;
		
		public JTextAreaStream(JTextArea area) {
			this.area=area;
			this.buf=new ByteArrayOutputStream();
			area.setEditable(false);
			area.getDocument().addDocumentListener(new DocumentListener() {
			  @Override public void insertUpdate(DocumentEvent e) {
			    final Document doc =area.getDocument();
			    final javax.swing.text.Element root = doc.getDefaultRootElement();
			    if (root.getElementCount() <= maxLen) return;
			    EventQueue.invokeLater(new Runnable() {
			      @Override public void run() {
			        removeLines(doc, root);
			      }
			    });
			    area.setCaretPosition(doc.getLength());
			  }
			  private void removeLines(Document doc, Element root) {
			    Element fl = root.getElement(0);
			    try {
			      doc.remove(0, fl.getEndOffset());
			    } catch (BadLocationException ble) {
			      System.out.println(ble);
			    }
			  }
			  @Override public void removeUpdate(DocumentEvent e) {}
			  @Override public void changedUpdate(DocumentEvent e) {}
			});
		}

		@Override
		public void write(int b) throws IOException {
			buf.write(b);
		}

		@Override
		public void flush() throws IOException {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					area.append(buf.toString());
					buf.reset();
				}
			});
	    }
	}
	
	
	class DropJTextField extends JTextField{

		private static final long serialVersionUID = 1L;
		public DropJTextField() {
			super();
			DropTargetListener dtl = new DropTargetAdapter() {
				  @Override
				  public void dragOver(DropTargetDragEvent dtde) {
				    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				      dtde.acceptDrag(DnDConstants.ACTION_COPY);
				      return;
				    }
				    dtde.rejectDrag();
				  }

				  @SuppressWarnings("rawtypes")
				  @Override
				  public void drop(DropTargetDropEvent dtde) {
				    try {
				      if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				        dtde.acceptDrop(DnDConstants.ACTION_COPY);
				        Transferable transferable = dtde.getTransferable();
				        List list = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
				        for (Object o: list) {
				          if (o instanceof File) {
				            File file = (File) o;
				            setText(file.getAbsolutePath());
				            break;
				          }
				        }
				        dtde.dropComplete(true);
				        return;
				      }
				    } catch (UnsupportedFlavorException | IOException ex) {
				      ex.printStackTrace();
				    }
				    dtde.rejectDrop();
				  }
				};
				new DropTarget(this, DnDConstants.ACTION_COPY, dtl, true);
		}
	}
	
	public void buildMvtFromJson(File gmlDir,File outDir,int minZoom,int maxZoom,String layerName) throws ParseException {
		System.out.println("- geojson -> mvt ---------------------");
		int n=gmlDir.listFiles().length;
		int iter=0;
		for(File f : gmlDir.listFiles()) {
			if(runner==null)return;
			iter++;
			if(f.isDirectory())continue;
			String name=f.getName();
			if(name.endsWith(".geojson")||name.endsWith(".json")) {
				try {
					System.out.println(f.getName()+" ("+Integer.toString(iter)+"/"+Integer.toString(n)+")");
					Map<String,List<Geometry>> tmp=loadGeojson(f,layerName);
					for(String key : tmp.keySet()) {
						List<Geometry> geom=tmp.get(key);
						Rectangle2D rect=GMLToJsonUtil.getBounds(geom);
						if(rect==null)continue;	
						MVTBuilder builder=new MVTBuilder(geom,rect,key);
						builder.createMVTs(minZoom, maxZoom, outDir);
					}
				} catch (ParseException | IOException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("- completed ----------------------");
	}
	
	private Map<String,List<Geometry>> loadGeojson(File f,String layer) throws IOException, ParseException{
		Map<String,List<Geometry>> ret=new HashMap<>();
		FeatureCollection fc=FeatureCollection.fromJson(readJson(f));
		List<Geometry> list=new ArrayList<>();
		for(Feature fe : fc.features()) {
			com.mapbox.geojson.Geometry mg=fe.geometry();
			if(mg instanceof MultiPolygon) {
				MultiPolygon mp=(MultiPolygon)mg;
				JsonObject jo=fe.properties();
				for(com.mapbox.geojson.Polygon cp : mp.polygons()) {
					Geometry g=toJTS(cp);
					g.setUserData(jo.deepCopy());
					list.add(g);
				}
			}else {
				Geometry g=toJTS(mg);
				g.setUserData(fe.properties());
				list.add(g);
			}
		}
		ret.put(layer, list);
		return ret;
	}
	
	public static Area toShape(Geometry g) {
		return new Area(sw.toShape(g));
	}
	
	public static Geometry toJTS(com.mapbox.geojson.Geometry g) throws ParseException {
		if(g instanceof Polygon) {
			Polygon p=(Polygon)g;
			if(!isAntieClockWise(p.coordinates().get(0))) {
				List<List<Point>> nt=counter(p.coordinates());
				g=Polygon.fromLngLats(nt);
			}
		}
		org.locationtech.jts.geom.Geometry geom=gr.read(g.toJson());
		return geom;
	}
	
	private static List<List<Point>> counter(List<List<Point>> li){
		List<List<Point>> ret=new ArrayList<>();
		for(List<Point> p : li) {
			ret.add(counterList(p));
		}
		return ret;
	}
	
	private static List<Point> counterList(List<Point> l){
		List<Point> ret=new ArrayList<>();
		while(l.size()>0) {
			ret.add(l.remove(l.size()-1));
		}
		return ret;
	}
	
	private static boolean isAntieClockWise(List<Point> p) {
		Point p1=p.get(0);
		Point p2=p.get(1);
		Point p3=p.get(2);
		double[] v1=new double[] {p2.longitude()-p1.longitude(),p2.latitude()-p1.latitude(),0};
		double[] v2=new double[] {p3.longitude()-p1.longitude(),p3.latitude()-p1.latitude(),0};
		double[] pd=crossProduct(v1,v2);
		if(pd[2]<0) {
			return false;
		}else {
			return true;
		}
	}
	
	private static double[] crossProduct(double[] a, double[] b) {  
	    double[] entries = new double[] {
	          a[1] * b[2] - a[2] * b[1],
	          a[2] * b[0] - a[0] * b[2],
	          a[0] * b[1] - a[1] * b[0]};
	    return entries;
	}
	
	private static String readJson(File f) throws IOException {
		StringBuffer buf=new StringBuffer();
		String line=null;
		BufferedReader br=new BufferedReader(new FileReader(f));
		while((line=br.readLine())!=null) {
			buf.append(line);
		}
		br.close();
		return buf.toString();
	}
	
	private static float[][] dchm(BufferedImage dsm,BufferedImage dem){
		float[][] v1=TileMeshUtil.getDM(dsm);
		float[][] v2=TileMeshUtil.getDM(dem);
		float[][] ret=new float[v1.length][v1[0].length];
		for(int i=0;i<v1.length;i++) {
			for(int j=0;j<v1[0].length;j++) {
				float vv=v1[i][j]-v2[i][j];
				if(Float.isNaN(vv))continue;
				if(vv>=0)ret[i][j]=vv;
			}
		}
		return ret;
	}
	
	
	public static void main(String[] args) {
		BuildingMvtApp app=new BuildingMvtApp();
		app.frame.setVisible(true);
	}
}
