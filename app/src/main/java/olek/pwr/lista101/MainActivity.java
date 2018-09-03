package olek.pwr.lista101;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import static java.lang.String.valueOf;

public class MainActivity extends Activity  {

    private SensorManager mySensorMenager; //meganger czujnikow
    private boolean pomiar=false; //czy nacisnieto przycisk start
    private float aX, aY, aZ;
    private TextView poleAx, poleAy, poleAz;
    private PowerManager.WakeLock mWakeLock;
    Button btnStart, btnStop, btnReset;

    TextView text;
    int counter=0;
    private boolean firstUpdate=true;
    private float shakeTreshold=7f;
    private boolean shakeInitiated = false;
    private float xAccel, yAccel, zAccel;
    private float xPreviousAccel, yPreviousAccel, zPreviousAccel;


    private ArrayList sensorData;

    NumberFormat numberFormat=new DecimalFormat("0.000");
    private int licznik=0;
    private double startTime;
    private double Ts;
    private double NS2S = 0.000000001;



    private XYSeries xTSeria = new XYSeries("X{t}");
    private XYSeries yTSeria = new XYSeries("Y(t)");
    private XYSeries zTSeria = new XYSeries("Z(t)");

    private XYSeriesRenderer rendererXT;
    private XYSeriesRenderer rendererYT;
    private XYSeriesRenderer rendererZT;
    private XYMultipleSeriesRenderer mrenderer;
    private XYMultipleSeriesDataset dataset;
    private LinearLayout chartLayout;
    private GraphicalView chartView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mySensorMenager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mySensorMenager.registerListener(mySensorEventListener,mySensorMenager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

        PowerManager powerManager=(PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock=powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"My tag");

        btnStart=(Button) findViewById(R.id.btnStart);
        btnStop=(Button) findViewById(R.id.btnStop);
        btnReset=(Button) findViewById(R.id.btnReset);
        btnReset.setVisibility(View.GONE);
        btnStop.setVisibility(View.GONE);
        text = (TextView) findViewById(R.id.counterTV);
        poleAx=(TextView) findViewById(R.id.poleAx);
        poleAy=(TextView) findViewById(R.id.poleAy);
        poleAz=(TextView) findViewById(R.id.poleAz);
        chartLayout=(LinearLayout) findViewById(R.id.llv);

    }

    private final SensorEventListener mySensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (pomiar){

                aX=sensorEvent.values[0];
                aY=sensorEvent.values[1];
                aZ=sensorEvent.values[2];

                if (licznik==0){
                    startTime=sensorEvent.timestamp;
                }
                Ts=(sensorEvent.timestamp-startTime)*NS2S;


                AccelData data = new AccelData(Ts,aX,aY,aZ);
                sensorData.add(data);

                poleAx.setText(valueOf(numberFormat.format(aX)));
                poleAy.setText(valueOf(numberFormat.format(aY)));
                poleAz.setText(valueOf(numberFormat.format(aZ)));

                licznik++;


            updateAccelParameters(aX,aY,aZ);
            if ((!shakeInitiated)&&isAccelerationChanged()){
                shakeInitiated=true;
            }else if ((shakeInitiated)&&isAccelerationChanged()){
                executeShakeAction();

            }else if ((shakeInitiated)&&(!isAccelerationChanged())){
                shakeInitiated=false;
            }
            }

        }

        private void updateAccelParameters(float xNewAccel, float yNewAccel, float zNewAccel){
            if (firstUpdate){
                xPreviousAccel=xNewAccel;
                yPreviousAccel=yNewAccel;
                zPreviousAccel=zNewAccel;
                firstUpdate=false;
            }else {
                xPreviousAccel=xAccel;
                yPreviousAccel=yAccel;
                zPreviousAccel=zAccel;
            }
            xAccel=xNewAccel;
            yAccel=yNewAccel;
            zAccel=zNewAccel;
        }

        private boolean isAccelerationChanged() {
            float deltaX = Math.abs(xPreviousAccel - xAccel);
            float deltaY = Math.abs(yPreviousAccel - yAccel);
            float deltaZ = Math.abs(zPreviousAccel - zAccel);
            return (deltaX > shakeTreshold && deltaY > shakeTreshold)
                    || (deltaX > shakeTreshold && deltaZ > shakeTreshold)
                    || (deltaY > shakeTreshold && deltaZ > shakeTreshold);
        }

        private void executeShakeAction(){
            counter++;
            text.setText(String.valueOf(counter));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    public void startBtn(View view) {
        btnStart.setVisibility(View.GONE);
        btnStop.setVisibility(View.VISIBLE);
        pomiar=true;
        mWakeLock.acquire();
        btnReset.setVisibility(View.VISIBLE);
        sensorData = new ArrayList<AccelData>();
    }

    public void stopBtn(View view) {
        if (pomiar){
            btnStop.setVisibility(View.GONE);
            btnStart.setVisibility(View.VISIBLE);
            pomiar=false;
            mWakeLock.release();
            saveToFile(sensorData,"/Olek_Akcelerometr/","save1.txt");
            String sread=readFromFile("/Olek_Akcelerometr/","save1.txt");
            Log.e("Odczytany plik", sread);
            draw();


            if (counter!=0||aX!=0||aY!=0||aZ!=0){
                btnReset.setVisibility(View.VISIBLE);

            }else {
                btnReset.setVisibility(View.GONE);
            }
        }
    }

    public void resetBtn(View view) {

        counter=0;
        poleAx.setText(valueOf(0));
        poleAy.setText(valueOf(0));
        poleAz.setText(valueOf(0));
        text.setText(valueOf(0));
        stopBtn(view);
        btnReset.setVisibility(View.GONE);
        sensorData=null;
        licznik=0;

        onCreate(new Bundle());
        chartLayout.removeAllViews();
        xTSeria.clear();
        yTSeria.clear();
        zTSeria.clear();

    }





    private void saveToFile(ArrayList<AccelData> data, String  folder,String fileName) {


        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath()+ folder);
        dir.mkdirs();
        File file = new File(dir, fileName);

        String test=file.getAbsolutePath();
        Log.i("My","FILE LOCATION: " + test);


        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);


            for (int i = 0; i < data.size(); i++) {
                pw.println(data.get(i));

            }

            pw.flush();
            pw.close();
            f.close();


            Toast.makeText(getApplicationContext(),

                    "Data saved",

                    Toast.LENGTH_LONG).show();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("My", "******* File not found. Did you" +
                    " add a WRITE_EXTERNAL_STORAGE permission to the manifest?");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private String readFromFile(String folder, String fileName){

        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath()+ folder);
        File file = new File(dir,fileName);

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                String[]lines=line.split(",");
                double t=Double.valueOf(lines[0]);
                double x=Double.valueOf(lines[1]);
                double y=Double.valueOf(lines[2]);
                double z=Double.valueOf(lines[3]);

                xTSeria.add(t,x);
                yTSeria.add(t,y);
                zTSeria.add(t,z);
                text.append('\n');
            }
            br.close();

        }

        catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("My", "******* File not found. Did you" +
                    " add a READ_EXTERNAL_STORAGE permission to the manifest?");
        }

        catch (IOException e) {
        }
        finally {
            return  text.toString();
        }
    }


    public void draw(){
        dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(xTSeria);
        dataset.addSeries(yTSeria);
        dataset.addSeries(zTSeria);

        rendererXT = new XYSeriesRenderer();
        rendererXT.setLineWidth(2);
        rendererXT.setColor(Color.BLUE);
        rendererXT.setPointStyle(PointStyle.CIRCLE);

        rendererYT = new XYSeriesRenderer();
        rendererYT.setLineWidth(2);
        rendererYT.setColor(Color.RED);
        rendererYT.setPointStyle(PointStyle.CIRCLE);

        rendererZT = new XYSeriesRenderer();
        rendererZT.setLineWidth(2);
        rendererZT.setColor(Color.GREEN);
        rendererZT.setPointStyle(PointStyle.CIRCLE);

        mrenderer = new XYMultipleSeriesRenderer();
        mrenderer.addSeriesRenderer(rendererXT);
        mrenderer.addSeriesRenderer(rendererYT);
        mrenderer.addSeriesRenderer(rendererZT);
        mrenderer.setYAxisMax(20);
        mrenderer.setYAxisMin(-20);
        mrenderer.setShowGrid(true);
        mrenderer.setMarginsColor(Color.WHITE);
        mrenderer.setGridColor(Color.LTGRAY);
        mrenderer.setAxesColor(Color.BLACK);
        mrenderer.setXLabelsColor(Color.BLACK);
        mrenderer.setYLabelsColor(0,Color.BLACK);
        mrenderer.setYLabelsAlign(Paint.Align.RIGHT);
        mrenderer.setLabelsTextSize(15);
        mrenderer.setLegendTextSize(15);

        if (xTSeria.getMaxY()>yTSeria.getMaxY()&&xTSeria.getMaxY()>zTSeria.getMaxY()){
            mrenderer.setYAxisMax(xTSeria.getMaxY());
        }
        if (yTSeria.getMaxY()>xTSeria.getMaxY()&&yTSeria.getMaxY()>zTSeria.getMaxY()){
            mrenderer.setYAxisMax(yTSeria.getMaxY());
        }
        if (zTSeria.getMaxY()>xTSeria.getMaxY()&&zTSeria.getMaxY()>yTSeria.getMaxY()){
            mrenderer.setYAxisMax(zTSeria.getMaxY());
        }


        if (xTSeria.getMinY()<yTSeria.getMinY()&&xTSeria.getMinY()<zTSeria.getMinY()){
            mrenderer.setYAxisMin(xTSeria.getMinY());
        }
        if (yTSeria.getMinY()<xTSeria.getMinY()&&yTSeria.getMinY()<zTSeria.getMinY()){
            mrenderer.setYAxisMin(yTSeria.getMinY());
        }
        if (zTSeria.getMinY()<xTSeria.getMinY()&&zTSeria.getMinY()<yTSeria.getMinY()){
            mrenderer.setYAxisMin(zTSeria.getMinY());
        }

        if (xTSeria.getMaxX()>yTSeria.getMaxX()&&xTSeria.getMaxX()>zTSeria.getMaxX()){
            mrenderer.setXAxisMax(xTSeria.getMaxX());
        }
        if (yTSeria.getMaxX()>xTSeria.getMaxX()&&yTSeria.getMaxX()>zTSeria.getMaxX()){
            mrenderer.setXAxisMax(yTSeria.getMaxX());
        }
        if (zTSeria.getMaxX()>xTSeria.getMaxX()&&zTSeria.getMaxX()>yTSeria.getMaxX()){
            mrenderer.setXAxisMax(zTSeria.getMaxX());
        }


        if (xTSeria.getMinX()<yTSeria.getMinX()&&xTSeria.getMinX()<zTSeria.getMinX()){
            mrenderer.setXAxisMin(xTSeria.getMinX());
        }
        if (yTSeria.getMinX()<xTSeria.getMinX()&&yTSeria.getMinX()<zTSeria.getMinX()){
            mrenderer.setXAxisMin(yTSeria.getMinX());
        }
        if (zTSeria.getMinX()<xTSeria.getMinX()&&zTSeria.getMinX()<yTSeria.getMinX()){
            mrenderer.setXAxisMin(zTSeria.getMinX());
        }


        dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(xTSeria);
        dataset.addSeries(yTSeria);
        dataset.addSeries(zTSeria);


        chartView = ChartFactory.getLineChartView(this, dataset, mrenderer);
        chartLayout.addView(chartView);
        chartView.repaint();
    }


}
