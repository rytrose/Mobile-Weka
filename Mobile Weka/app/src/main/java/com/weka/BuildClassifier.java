package com.weka;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.RBFNetwork;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.rules.ZeroR;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class BuildClassifier extends TabActivity{
	
	public static final int ITEM0 = Menu.FIRST;
	public static final int ITEM1 = Menu.FIRST + 1;
	
	private static final int promptDialog = 1;
	private static final int FileDetailDialog = 2;
	private static final int SaveModelDialog = 3;
	private static final int ChooseFileDialog = 4;
	
	private static final int NAIVEBAYES = 0;
	private static final int RBFNETWORK = 1;
	private static final int J48 = 2;
	private static final int ADABOOSTM1 = 3;
	private static final int ZEROR = 4;
	private static final int LinearRegression = 5;
	
	private List<String> items = null;
	private List<String> paths = null;
	private String rootPath = "/";
	private String curPath = "/"; 
	private int TypeFile = 0;
	private ListView list;
	private View vv;
	private TabHost tabHost;
	
	private String TrainfileName, TestfileName, ModelPathName;
	private String classifierString, TestSummaryString = "", TrainSummaryString = "";
	private String TestSummaryresult = "", MatrixString = "", ClassDetailsString = "";
	
	private Spinner spinner_1, spinner_2;
	private TextView classifier_trainflie, classifier_testfile;
	private TextView model_text, result_text, file_detail, show_state;
	private Button trainfiledetail, testfiledetail, choosetestfile, choosetrainfile;
	private Button Train, Test, LoadModel, SaveModel, SummaryButton,  MatrixButton, ClassDetailsButton;
	private ProgressBar progress;
	
	private String[] VAR;
	private static final String[]  ALG = { "NAIVEBAYES" ,"RBFNETWORK", "J48", "ADABOOSTM1","ZEROR", "LinearRegression"};
	private ArrayAdapter<String> aspn;
	
	private float time;
	private Timer timer;
	private NumberFormat df = NumberFormat.getInstance();
	
	private int chooseALG = 0, chooseVar = 0;	
	private Instances instancesTrain, instancesTest;
	private Classifier cfs = null;
	
	private int TypePrompt;
	private boolean isLoadModel = false;
	private boolean running = false;
	private boolean hasFalse = false;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);	
		tabHost = getTabHost();
		LayoutInflater.from(this).inflate(R.layout.buildclassifier,
				tabHost.getTabContentView(), true);
		tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("Choose")
				.setContent(R.id.chooseclassifier_layout));
		tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("Model")
				.setContent(R.id.classifiermodel_layout));
		tabHost.addTab(tabHost.newTabSpec("tab3").setIndicator("Test Result")
				.setContent(R.id.classifiertestresult_layout));
		
		tabHost.getTabWidget().getChildAt(1).setClickable(false);
		tabHost.getTabWidget().getChildAt(2).setClickable(false);
		final TabWidget tabWidget = tabHost.getTabWidget();		 
		for (int i =0; i < tabWidget.getChildCount(); i++) {  
			tabWidget.getChildAt(i).getLayoutParams().height = 30;  
		}
		
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(2);

		FindViewById();
		ClickListener();
		Initspinner_1();
		show_state.setText("");
		progress.setVisibility(View.GONE);
		showDialog(ChooseFileDialog);
	}
	
	private Handler mHandler = new Handler() {    
        public void handleMessage(Message msg) {  
        	switch (msg.what) {
        	case 1:
        		progress.setVisibility(View.GONE);
				show_state.setText("  Open Complete.");
				if (TypeFile == 0) {
					InitVar();
					Initspinner_2();
				}else if (TypeFile == 2){
					tabHost.getTabWidget().getChildAt(1).setClickable(true);
					tabHost.getTabWidget().getChildAt(2).setClickable(false);
					tabHost.setCurrentTab(1);
					classifierString = cfs.toString();
					model_text.setText(classifierString);
				}
        		break;
        	case 2:        		
        		progress.setVisibility(View.GONE);
        		if (hasFalse) {
        			show_state.setText("Train Field,use time:"+df.format(time)+"s");
        			showDialog(promptDialog);
        			break;

        		}
        		show_state.setText("Train Complete,use time:"+df.format(time)+"s");	
        		model_text.setText(classifierString);
        		tabHost.getTabWidget().getChildAt(1).setClickable(true);
        		tabHost.setCurrentTab(1);
        		break;
        	case 3:
        		time += 0.01;
        		show_state.setText("  Training,use time:"+df.format(time)+"s");
        		break;
        	case 4:
        		time += 0.01;
        		show_state.setText("  Testing,use time:"+df.format(time)+"s");
        		break;
        	case 5:        		
        		progress.setVisibility(View.GONE);
        		if (hasFalse) {
        			show_state.setText("Test Field,use time:"+df.format(time)+"s");
        			showDialog(promptDialog);
        			break;
        		}
        		show_state.setText("Test Complete,use time:"+df.format(time)+"s");	
        		result_text.setText(TestSummaryresult);
        		tabHost.getTabWidget().getChildAt(2).setClickable(true);
        		tabHost.setCurrentTab(2);
        	}
            super.handleMessage(msg);    
         }    
     };
     
     public void Initspinner_1() {
     	aspn = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, ALG);
 		aspn.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
 		spinner_1.setAdapter(aspn);	
     }
    
    public void Initspinner_2() {
    	aspn = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, VAR);
 		aspn.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
 		spinner_2.setAdapter(aspn);
 		spinner_2.setSelection(chooseVar);
    }
    
    public void InitVar() {
    	VAR = new String[instancesTrain.numAttributes()];
		for (int i = 0; i < instancesTrain.numAttributes(); i++)
			VAR[i] = instancesTrain.attribute(i).name();
		chooseVar = instancesTrain.numAttributes() - 1;
    }
	
	public void Readfile() {
    	try {
    		if (TypeFile == 0) {
	        	File file = new File(TrainfileName);	
	    		ArffLoader atf = new ArffLoader(); 
				atf.setFile(file);
				instancesTrain = atf.getDataSet();
				TrainSummaryString = instancesTrain.toSummaryString();
    		}else if (TypeFile == 1) {
    			File file = new File(TestfileName);	
	    		ArffLoader atf = new ArffLoader(); 
				atf.setFile(file);
				instancesTest = atf.getDataSet();
				TestSummaryString = instancesTest.toSummaryString();
			}else if (TypeFile == 2) {
				ObjectInputStream ois=new ObjectInputStream(new FileInputStream(ModelPathName));
		        cfs = (Classifier)ois.readObject();
		        ois.close();
		        isLoadModel = true;
			}
    		
		} catch (IOException e) {} 
    	catch (ClassNotFoundException e) {}
    }
	
	public void Train() {
		switch (chooseALG) {
		case NAIVEBAYES:
			cfs = new NaiveBayes();
			break;
		case RBFNETWORK:
			cfs = new RBFNetwork();
			break;
		case J48:
			cfs = new J48();
			break;
		case ADABOOSTM1:
			cfs = new AdaBoostM1();
			break;
		case ZEROR:
			cfs = new ZeroR();
			break;
		case LinearRegression:
			cfs = new LinearRegression();
			break;
		}
		instancesTrain.setClassIndex(chooseVar);
		try {
			cfs.buildClassifier(instancesTrain);
			classifierString = cfs.toString();
			isLoadModel = false;
		} catch (Exception e) {
			TypePrompt = 5;
			hasFalse = true;
		}
		
	}
	
	public void Test() {
		if (isLoadModel) {
			for (int i = 0; i < instancesTest.numAttributes(); i++) {
		           try {
		        	   instancesTest.setClassIndex(i);
		           Evaluation testingEvaluation = new Evaluation(instancesTest);
		           Instance testInst = instancesTest.instance(0);
		              testingEvaluation.evaluateModelOnceAndRecordPrediction(
		                  cfs, testInst);
		              chooseVar = i;
		           }catch(Exception e){}
	           }
		}
		instancesTest.setClassIndex(chooseVar);
	   Instance testInst;
	   Evaluation testingEvaluation = null;
	try {
		testingEvaluation = new Evaluation(instancesTest);
	   int length = instancesTest.numInstances(); 
	   for (int i =0; i < length; i++) {
	      testInst = instancesTest.instance(i);
	      testingEvaluation.evaluateModelOnceAndRecordPrediction(cfs, testInst);
	   }  
	   TestSummaryresult = "Recall Rate                " +  testingEvaluation.recall(0);
	   TestSummaryresult += "\nPrecision Rate            " +  testingEvaluation.precision(0);
	   TestSummaryresult += "\nF1                        " +  testingEvaluation.fMeasure(0);
	   TestSummaryresult += testingEvaluation.toSummaryString();
	   MatrixString = testingEvaluation.toMatrixString();
	   ClassDetailsString = testingEvaluation.toClassDetailsString(); 
	   } catch (Exception e) {
		   TypePrompt = 3;
		   hasFalse = true;
	   }
	}
	
	public void ClickListener() {
	    	OnClickListener listener = new OnClickListener() {
				public void onClick(View v) {
					if (running == true) {	
						TypePrompt = 2;
						showDialog(promptDialog);
					}else if (v == choosetrainfile) {
						TypeFile = 0;
						showDialog(ChooseFileDialog);
					}else if (v == choosetestfile) {
						TypeFile = 1;
						showDialog(ChooseFileDialog);
					}else if (v == trainfiledetail && instancesTrain != null) {
						TypeFile = 0;
						showDialog(FileDetailDialog);
					}else if (v == testfiledetail && instancesTest != null) {
						TypeFile = 1;
						showDialog(FileDetailDialog);
					}else if (v == Train) {	
						running = true;
						hasFalse = false;
						progress.setVisibility(View.VISIBLE);
						show_state.setText("  Training,Waiting.");						
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
				            	if (instancesTrain != null)
				            		Train();
				            	timer.cancel();
								running = false;
				            	Message message = Message.obtain(); 
				                message.what = 2;  
				                mHandler.sendMessage(message);  
				            }       
				         }){ }.start(); 
					}else if (v == Test) {
						if (instancesTest == null || cfs == null) {
							TypePrompt = 6;
							showDialog(promptDialog);
						}else {
							running = true;
							hasFalse = false;
							progress.setVisibility(View.VISIBLE);
							show_state.setText("  Testing,Waiting.");						
							time = 0;
							timer = new Timer(true);
							TimerTask task = new TimerTask(){  
							      public void run() {  
							      Message message = new Message();      
							      message.what = 4; 
							      mHandler.sendMessage(message);  
							    }  
							 };
							timer.schedule(task, 0, 10);
							new Thread(new Runnable(){    	   
					            @Override   
					            public void run() {
					            	if (instancesTest != null)
					            		Test();
					            	timer.cancel();
									running = false;
					            	Message message = Message.obtain(); 
					                message.what = 5;  
					                mHandler.sendMessage(message);  
					            }       
					         }){ }.start(); 
						}
					}else if (v == LoadModel) {	
						TypeFile = 2;
						showDialog(ChooseFileDialog);
					}else if (v == SaveModel) {	
						showDialog(SaveModelDialog);
					}else if (v == SummaryButton) {
						result_text.setText(TestSummaryresult);
						
					}else if (v == MatrixButton) {
						result_text.setText(MatrixString);
						
					}else if (v == ClassDetailsButton) {
						result_text.setText(ClassDetailsString);
					}
				}
	    		
	    	};	    	
	    	choosetrainfile.setOnClickListener(listener);
	    	choosetestfile.setOnClickListener(listener);
	    	trainfiledetail.setOnClickListener(listener);
	    	testfiledetail.setOnClickListener(listener);
	    	Train.setOnClickListener(listener);
	    	Test.setOnClickListener(listener);
	    	SummaryButton.setOnClickListener(listener);
	    	MatrixButton.setOnClickListener(listener);
	    	ClassDetailsButton.setOnClickListener(listener);
	    	SaveModel.setOnClickListener(listener);
	    	LoadModel.setOnClickListener(listener);
	    	
	    	spinner_1.setOnItemSelectedListener(new OnItemSelectedListener() {
	 			public void onItemSelected(AdapterView<?> arg0, View arg1,
	 					int arg2, long arg3) {
	 				chooseALG = arg2;
	 			}
	 			public void onNothingSelected(AdapterView<?> arg0) {
	 				chooseALG = 0;				
	 			}		
	 		});
	    	
	    	spinner_2.setOnItemSelectedListener(new OnItemSelectedListener() {
	 			public void onItemSelected(AdapterView<?> arg0, View arg1,
	 				int arg2, long arg3) {
	 				chooseVar = arg2;
	 			}
	 			public void onNothingSelected(AdapterView<?> arg0) {
	 				chooseVar = 0;	
	 			}		
	 		});
	    }
	
	public void FindViewById() {
    	classifier_trainflie = (TextView)findViewById(R.id.classifier_trainflie);
    	classifier_testfile = (TextView)findViewById(R.id.classifier_testfile);
    	choosetrainfile = (Button)findViewById(R.id.choosetrainfile);
    	choosetestfile = (Button)findViewById(R.id.choosetestfile);
    	trainfiledetail = (Button)findViewById(R.id.trainfiledetail);
    	testfiledetail = (Button)findViewById(R.id.testfiledetail);
    	model_text = (TextView)findViewById(R.id.model_text);
    	result_text = (TextView)findViewById(R.id.testresutl_text);
    	show_state = (TextView)findViewById(R.id.show_state);
    	spinner_1 = (Spinner)findViewById(R.id.spinner_1);  
    	spinner_2 = (Spinner)findViewById(R.id.spinner_2);
    	Train = (Button)findViewById(R.id.train_button);
    	Test = (Button)findViewById(R.id.test_button);
    	SummaryButton = (Button)findViewById(R.id.SummaryString);
    	LoadModel = (Button)findViewById(R.id.load_model);
    	SaveModel = (Button)findViewById(R.id.save_model);
    	MatrixButton = (Button)findViewById(R.id.MatrixString);
    	ClassDetailsButton = (Button)findViewById(R.id.ClassDetailsString);
    	progress = (ProgressBar)findViewById(R.id.ProgressBar);
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
			if (file.getName().endsWith(".arff") || file.isDirectory() || file.getName().endsWith(".model")) {
				items.add(file.getName());
				paths.add(file.getPath());
			}
		}
		list.setAdapter(new FileAdapter(this, items, paths));
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
		builder.setTitle("Choose Train File");
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
				if (curPath.endsWith(".arff")&&(TypeFile == 0||TypeFile == 1)) {
					running = true;
					dismissDialog(ChooseFileDialog);
					progress.setVisibility(View.VISIBLE);
					show_state.setText("  Opening File,Waiting.");
					if (TypeFile == 0) {
						TrainfileName = curPath;
						classifier_trainflie.setText(TrainfileName);
					}
					else if (TypeFile == 1) {
						TestfileName = curPath;
						classifier_testfile.setText(TestfileName);
					}
					new Thread(new Runnable(){    	   
			            @Override   
			            public void run() {
			            	Readfile();
			            	Message message = Message.obtain(); 
			                message.what = 1;  
			                mHandler.sendMessage(message);  
			                running = false;
			            }       
			         }){ }.start();    	  
				}else if (curPath.endsWith(".model")&&TypeFile == 2) {
					running = true;
					dismissDialog(ChooseFileDialog);
					ModelPathName = curPath;
					new Thread(new Runnable(){    	   
			            @Override   
			            public void run() {
			            	Readfile();
			            	Message message = Message.obtain(); 
			                message.what = 1;  
			                mHandler.sendMessage(message);  
			                running = false;
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
				if (TrainfileName == null){
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
		builder.setTitle("Train file can not be null.");
		builder.setIcon(R.drawable.warn);
		builder.setPositiveButton("ok", null);		
		return builder.create();
	}
    
    private Dialog SaveModelDialog(Context context) {
    	LayoutInflater inflater = LayoutInflater.from(this);
		final View textEntryView = inflater.inflate(
				R.layout.savemodel, (ViewGroup)findViewById(R.id.savemodel));
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		final EditText edit = (EditText)textEntryView.findViewById(R.id.savePath_edit);
		edit.setText("/sdcard/");
		builder.setTitle("Save Path");
		builder.setIcon(R.drawable.weka);	
		builder.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						ObjectOutputStream oos;
						try {
							oos = new ObjectOutputStream(new FileOutputStream(edit.getText()+".model"));
							oos.writeObject(cfs);
				            oos.flush();
				            oos.close();
						} catch (FileNotFoundException e) {} 
						catch (IOException e) {}
					}
				});
		builder.setNegativeButton("Cancle", null);
		builder.setView(textEntryView);
		return builder.create();
	}
    
	protected Dialog onCreateDialog(int id) {
		if (id == ChooseFileDialog)
			return ChooseFileDialog(BuildClassifier.this);
		else if (id == FileDetailDialog)
			return FileDetailDialog(BuildClassifier.this);
		else if (id == promptDialog)
			return promptDialog(BuildClassifier.this);
		else if (id == SaveModelDialog) {
			return SaveModelDialog(BuildClassifier.this);
		}
		return null;	
	}
	
	protected void onPrepareDialog(int id, Dialog dialog){
		if (id == ChooseFileDialog){
			if (TypeFile == 0) {
				dialog.setTitle("Choose Train File");
			}else if (TypeFile == 1){
				dialog.setTitle("Choose Test File");
			}else if (TypeFile == 2){
				dialog.setTitle("Choose Model");
			}
		}else if (id == FileDetailDialog){
			if (TypeFile == 0) {
				dialog.setTitle("Train File");
				file_detail.setText(TrainSummaryString);
			}else if (TypeFile == 1){
				dialog.setTitle("Test File");
				file_detail.setText(TestSummaryString);
			}
		}else if (id == promptDialog){
			if (TypePrompt == 0) {
				dialog.setTitle("Please choose right file.");
			}else if (TypePrompt == 1){
				dialog.setTitle("Train file can not be null.");
			}else if (TypePrompt == 2){
				dialog.setTitle("It is Running now.Do it a moment later.");
			}else if (TypePrompt == 3){
				dialog.setTitle("Test file is not match the model.");
			}else if (TypePrompt == 4){
				dialog.setTitle("Test file is not match the train file.");
			}else if (TypePrompt == 5){
				dialog.setTitle("Train file is not match the classifier.");
			}else if (TypePrompt == 6){
				dialog.setTitle("Test file or Classifer is null.");
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
		intent.setClass(BuildClassifier.this, MainMenu.class);
		startActivity(intent);
	}
}
