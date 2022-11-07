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
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;

import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.MultiPolygon;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class Geojson2MvtApp {
	private JFrame frame;
	private JFileChooser ch;
	private DropJTextField src,dst;
	private JComboBox<Integer> zoom01,zoom02;
	private JTextField check;
	private JButton exec;
	private Thread runner;
	private static GeoJsonReader gr=new GeoJsonReader();
	
	public Geojson2MvtApp() {
		frame=new JFrame();
		frame.setTitle("Geojson2Mvt");
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
		JPanel pup=new JPanel(new GridLayout(4,1));
		frame.getContentPane().add(pup,BorderLayout.NORTH);
		JPanel p1=new JPanel(new BorderLayout());
		p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "■入力先（Geojson）"));
		src=new DropJTextField();
		src.setBorder(BorderFactory.createLoweredBevelBorder());
		src.setEditable(false);
		p1.add(src,BorderLayout.CENTER);		
		JButton bt1=new JButton("－");
		bt1.addActionListener(e->{
			File fo=new File(src.getText());
			if(fo.exists())ch.setSelectedFile(fo);
			int ck=ch.showOpenDialog(frame);
			if(ck==JFileChooser.APPROVE_OPTION) {
				src.setText(ch.getSelectedFile().getAbsolutePath());
			}
		});
		p1.add(bt1,BorderLayout.EAST);
		pup.add(p1);
		JPanel p2=new JPanel(new BorderLayout());
		p2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "■出力先（MVT）"));
		dst=new DropJTextField();
		dst.setBorder(BorderFactory.createLoweredBevelBorder());
		dst.setEditable(false);
		p2.add(dst,BorderLayout.CENTER);
		pup.add(p2);
		JButton bt2=new JButton("－");
		bt2.addActionListener(e->{
			File fo=new File(src.getText());
			if(fo.exists())ch.setSelectedFile(fo);
			int ck=ch.showOpenDialog(frame);
			if(ck==JFileChooser.APPROVE_OPTION) {
				dst.setText(ch.getSelectedFile().getAbsolutePath());
			}
		});
		p2.add(bt2,BorderLayout.EAST);
		JPanel panel02=new JPanel(new GridLayout(1,3));
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
		check=new JTextField("NULL");
		JPanel tmp=new JPanel(new BorderLayout());
		tmp.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "レイヤー名"));
		tmp.add(check,BorderLayout.CENTER);
		panel02.add(tmp);
		
		exec=new JButton("処理開始");
		exec.addActionListener(e->{
			if(runner==null) {
				int ck=JOptionPane.showConfirmDialog(frame, "実行しますか？", "確認", JOptionPane.YES_NO_OPTION);
				if(ck==JOptionPane.YES_OPTION) {
					File inDir=new File(src.getText());
					if(inDir.exists()) {
						File outDir=new File(dst.getText());
						if(outDir.exists()) {
							int minZoom=(Integer)zoom01.getSelectedItem();
							int maxZoom=(Integer)zoom02.getSelectedItem();
							exec.setForeground(Color.RED);
							exec.setText("処理中止");
							Runnable r=new Runnable() {
								public void run() {
									try {
										buildMvtFromJson(inDir,outDir,minZoom,maxZoom,check.getText());
									} catch (ParseException e1) {
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
					}
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
	
	public static void main(String[] args) {
		Geojson2MvtApp app=new Geojson2MvtApp();
		app.frame.setVisible(true);
	}
}
