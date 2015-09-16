package com.weka;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import weka.associations.AbstractAssociator;
import weka.associations.FilteredAssociator;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class BuildAssociate extends TabActivity{
	
	private static final int FileDetailDialog = 1;
	private static final int ChooseFileDialog = 2;
	private static final int promptDialog = 3;
	
	private static final int Apriori = 0;
	private static final int FilteredAssociator = 1;
	private static final int FPGrowth = 2;
	private static final int Tertius = 3;
	
	public static final int ITEM0 = Menu.FIRST;
	public static final int ITEM1 = Menu.FIRST + 1;
	
	private TextView clusterer_file, result_text, file_detail, show_state;
	private Button start, filedetail, choosefile;
	private ProgressBar progress;
	private Spinner spinner_4;
	private TabHost tabHost;
	
	private String fileName, fileSummaryString, resutl_string;
	
	private ArrayAdapter<String> aspn;
	private int chooseALG = 0;
	private String[] ALG = {"Apriori", "FilteredAssociator", "FPGrowth", "Tertius"};
	
	private Timer timer;
	private float time;
	private NumberFormat df = NumberFormat.getInstance();
	
	
	private List<String> items = null;
	private List<String> paths = null;
	private String rootPath = "/";
	private String curPath = "/"; 
	private ListView list;
	private View vv;
	private int TypePrompt;
	private boolean hasFalse = false;
	private boolean running = false;

	private Instances instances;	
	
	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);	
		tabHost = getTabHost();
		LayoutInflater.from(this).inflate(R.layout.buildassociate,
				tabHost.getTabContentView(), true);
		tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("Choose")
				.setContent(R.id.chooseassociate_layout));
		tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("Result")
				.setContent(R.id.associate_result));  
		
		final TabWidget tabWidget = tabHost.getTabWidget();		 
		for (int i =0; i < tabWidget.getChildCount(); i++) {  
			tabWidget.getChildAt(i).getLayoutParams().height = 30;  
		}
		findViewById();
		Initspinner_4();
		Clicklistener();
		tabHost.getTabWidget().getChildAt(1).setClickable(false);
		show_state.setText("");
		progress.setVisibility(View.GONE);
		showDialog(ChooseFileDialog);
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(2);
    }
    
    private Handler mHandler = new Handler() {    
        public void handleMessage(Message msg) {  
        	switch (msg.what) {
        	case 1:
        		progress.setVisibility(View.GONE);
        		show_state.setText("Open Complete,use time:"+df.format(time)+"s");
        		break;
        	case 2:
        		start.setText("Start");
        		progress.setVisibility(View.GONE);
        		if (hasFalse) {
        			show_state.setText("Build Fail,use time:"+df.format(time)+"s");
        			TypePrompt = 3;
        			showDialog(promptDialog);
        		}else{
        			show_state.setText("Build Complete,use time:"+df.format(time)+"s");
        			showResult();
        			tabHost.getTabWidget().getChildAt(1).setClickable(true);
        			tabHost.setCurrentTab(1);
        		}
        		break;
        	case 3:
        		time += 0.01;
        		show_state.setText("  Running,use time:"+df.format(time)+"s");
        		break;
        	}
            super.handleMessage(msg);    
         }    
     };
    
    public void findViewById() {
    	filedetail = (Button)findViewById(R.id.associatefiledetails);
    	choosefile = (Button)findViewById(R.id.associatechoosefile);
    	clusterer_file = (TextView)findViewById(R.id.associate_file);
    	result_text = (TextView)findViewById(R.id.associateresult_text);
    	start = (Button)findViewById(R.id.start2);
    	spinner_4 = (Spinner)findViewById(R.id.spinner_4);
    	show_state = (TextView)findViewById(R.id.show_state2);
    	progress = (ProgressBar)findViewById(R.id.ProgressBar2);
    }
    
    public void Initspinner_4() {
     	aspn = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, ALG);
 		aspn.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
 		spinner_4.setAdapter(aspn);	
     }
    
    public void Clicklistener() {
    	OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (running == true) {
					TypePrompt = 2;
					showDialog(promptDialog);
				}else if (v == filedetail) {
					showDialog(FileDetailDialog);
				}else if (v == choosefile) {
					showDialog(ChooseFileDialog);
				}else if (v == start) {
					running = true;
					hasFalse = false;
					time = 0;
					timer = new Timer(true);
					TimerTask task = new TimerTask(){  
					      public void run() {  
					      Message message = new Message();      
					      message.what = 3; 
					      mHandler.sendMessage(message);  
					    }  
					 };
					timer.schedule(task, 0, 10);
					
					new Thread(new Runnable(){    	   
	                    @Override   
	                    public void run() { 
	                    	build();
	                    	timer.cancel();
	                    	Message message = Message.obtain();
	                    	message.what = 2;  
	                        mHandler.sendMessage(message);  
	                        running = false;
	                    }       
	                 }){ }.start(); 
				}
			}
    	};
    	filedetail.setOnClickListener(listener);
    	choosefile.setOnClickListener(listener);
    	start.setOnClickListener(listener);
    	
    	spinner_4.setOnItemSelectedListener(new OnItemSelectedListener() {
 			public void onItemSelected(AdapterView<?> arg0, View arg1,
 					int arg2, long arg3) {
 				chooseALG = arg2;
 			}
 			public void onNothingSelected(AdapterView<?> arg0) {
 				chooseALG = 0;				
 			}		
 		});
    }
    
    private void getFileDir(String filePath) {
		items = new ArrayList<String>();
		paths = new ArrayList<String>();
		File f = new File(filePath);
		File[] files = f.listFiles();
		if (!filePath.equals(rootPath)) {
			items.add("b1");
			paths.add(rootPath);
			items.add("b2");
			paths.add(f.getParent());
		}
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.getName().endsWith(".arff") || file.isDirectory()) {
				items.add(file.getName());
				paths.add(file.getPath());
			}
		}
		list.setAdapter(new FileAdapter(this, items, paths));
	}
    
    public void Readfile() {
    	try {
        	File file = new File(fileName);	
    		ArffLoader atf = new ArffLoader(); 
			atf.setFile(file);
			instances = atf.getDataSet();
			fileSummaryString = instances.toSummaryString();		
		} catch (IOException e) {} 
    }
    
    public void build() {
    	AbstractAssociator associate = null;
    	switch (chooseALG){
    	case Apriori:
    		associate = new weka.associations.Apriori();
    		break;
    	case FilteredAssociator:
    		associate = new FilteredAssociator();
    		break;
    	case FPGrowth:
    		associate = new weka.associations.FPGrowth();
    		break;
    	case Tertius:
    		associate = new weka.associations.Tertius();
    		break;
    	}
    	try {
			associate.buildAssociations(instances);
			resutl_string = associate.toString();
		} catch (Exception e) {
			hasFalse = true;
		}
    }
    
    public void showResult() {
    	result_text.setText(resutl_string);
    }
    
    private Dialog FileDetailDialog(Context context) {
    	LayoutInflater inflater = LayoutInflater.from(this);
		final View textEntryView = inflater.inflate(
				R.layout.seefile, (ViewGroup)findViewById(R.id.seefile_layout));
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Train File");
		builder.setIcon(R.drawable.weka);	
		file_detail = (TextView)textEntryView.findViewById(R.id.file_text);
		builder.setView(textEntryView);
		builder.setPositiveButton("OK", null);
		return builder.create();
	}
    
    private Dialog ChooseFileDialog (Context context) {
		LayoutInflater inflater = LayoutInflater.from(this);
		final View textEntryView = inflater.inflate(
				R.layout.choosefile, (ViewGroup)findViewById(R.id.choosefile_layout));
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Choose File");
		builder.setIcon(R.drawable.weka);
		list = (ListView)textEntryView.findViewById(R.id.list);
		getFileDir("/sdcard/");
		list.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
				File file = new File(paths.get(position));
				if (file.isDirectory()) {
					if (vv != null)
						vv.setBackgroundColor(Color.WHITE);
					curPath = paths.get(position);
					getFileDir(paths.get(position));
				} else {
					v.setBackgroundColor(Color.GRAY);
					if (vv != null)
						vv.setBackgroundColor(Color.WHITE);					
					vv = v;
					curPath = paths.get(position);
				}
			}
		});
		Button confirmbutton = (Button)textEntryView.findViewById(R.id.buttonfileConfirm);
		Button canclebutton = (Button)textEntryView.findViewById(R.id.buttonfileCancle);
		confirmbutton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if (curPath.endsWith(".arff")) {
					fileName = curPath;
					clusterer_file.setText(fileName);
					running = true;
					dismissDialog(ChooseFileDialog);
					progress.setVisibility(View.VISIBLE);
					show_state.setText("  Opening File,Waiting.");
					time = 0;
					timer = new Timer(true);
					TimerTask task = new TimerTask(){  
					      public void run() {  
					      Message message = new Message();      
					      message.what = 3; 
					      mHandler.sendMessage(message);  
					    }  
					 };
					timer.schedule(task, 0, 10);
					new Thread(new Runnable(){    	   
			            @Override   
			            public void run() {
			            	Readfile();
			            	Message message = Message.obtain(); 
			                message.what = 1;  
			                mHandler.sendMessage(message);  
			                running = false;
			                timer.cancel();
			            }       
			         }){ }.start(); 
				}else {
					TypePrompt = 0;
					showDialog(promptDialog);
				}
			}		
		});
		canclebutton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (fileName == null){
					TypePrompt = 1;
					showDialog(promptDialog);
				}else
					dismissDialog(ChooseFileDialog);				
			}
		});
		builder.setView(textEntryView);
		return builder.create();
	}
    
    private Dialog promptDialog(Context context) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("ddfdfd");
		builder.setIcon(R.drawable.warn);
		builder.setPositiveButton("ok", null);		
		return builder.create();
	}
    
    protected Dialog onCreateDialog(int id) {
		if (id == ChooseFileDialog)
			return ChooseFileDialog(BuildAssociate.this);
		else if (id == FileDetailDialog)
			return FileDetailDialog(BuildAssociate.this);
		else if (id == promptDialog)
			return promptDialog(BuildAssociate.this);
		return null;	
	}
	
	protected void onPrepareDialog(int id, Dialog dialog){
		if (id == FileDetailDialog){
			file_detail.setText(fileSummaryString);
		}else if (id == promptDialog){
			if (TypePrompt == 0) {
				dialog.setTitle("Please choose right file.");
			}else if (TypePrompt == 1){
				dialog.setTitle("File can not be null.");
			}else if (TypePrompt == 2){
				dialog.setTitle("It is Running now.Do it a moment later.");
			}else if (TypePrompt == 3){
				dialog.setTitle("There is a false, when use associate.");
			}
		}
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, ITEM0, 0, "Exit");
		menu.add(0, ITEM1, 0, "Menu");
		menu.findItem(ITEM1);
		return true;
	}
    
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case ITEM0: 
			actionClickMenuItem1();
		break;
		case ITEM1: 
			actionClickMenuItem2(); 
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	private void actionClickMenuItem1(){
		Intent intent=new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}
	private void actionClickMenuItem2(){
		Intent intent = new Intent();
		Bundle bundle1 = new Bundle();					
		intent.putExtras(bundle1);
		intent.setClass(BuildAssociate.this, MainMenu.class);
		startActivity(intent);
	}
}