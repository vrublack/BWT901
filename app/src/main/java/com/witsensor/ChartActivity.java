package com.witsensor;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Pair;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


public class ChartActivity extends Activity {

    private LineChart mChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        mChart = findViewById(R.id.chart);
        mChart.setTouchEnabled(true);
        mChart.setPinchZoom(true);

        setUpCharts();
    }

    private void setUpCharts() {
        File[] files = getExternalFilesDir(null).listFiles();

        List<Pair<Date, File>> allDates = new ArrayList<>();
        for (File f : files) {
            if (!f.getName().startsWith("Recording__"))
                continue;
            try {
                String datePortion = f.getName().substring(11, f.getName().length() - 4);
                Date date = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss").parse(datePortion);
                allDates.add(new Pair<Date, File>(date, f));

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        // sort by date
        Collections.sort(allDates, new Comparator<Pair<Date, File>>() {
            @Override
            public int compare(Pair<Date, File> t1, Pair<Date, File> t2) {
                return t1.first.compareTo(t2.first);
            }
        });

        if (allDates.size() < 2) {
            Toast.makeText(this, "Need calibration file", Toast.LENGTH_LONG).show();
            return;
        }

        File calibrationFile = allDates.get(allDates.size() - 2).second;
        File dataFile = allDates.get(allDates.size() - 1).second;

        List<float[]> calibAngles = getAngles(calibrationFile);
        List<float[]> angles = getAngles(dataFile);

        ArrayList<Entry> xValues = new ArrayList<>();
        ArrayList<Entry> yValues = new ArrayList<>();
        ArrayList<Entry> zValues = new ArrayList<>();
        for (int i = 0; i < angles.size(); i++) {
            float seconds = angles.get(i)[0];
            xValues.add(new Entry(seconds, angles.get(i)[1]));
            yValues.add(new Entry(seconds, angles.get(i)[2]));
            zValues.add(new Entry(seconds, angles.get(i)[3]));
        }

        float calibX = 0;
        float calibY = 0;
        float calibZ = 0;
        for (int i = 0; i < calibAngles.size(); i++) {
            calibX += calibAngles.get(i)[1];
            calibY += calibAngles.get(i)[2];
            calibZ += calibAngles.get(i)[3];
        }
        calibX /= calibAngles.size();
        calibY /= calibAngles.size();
        calibZ /= calibAngles.size();

        makeChart(xValues, yValues, zValues, calibX, calibY, calibZ);
    }

    private List<float[]> getAngles(File file) {
        List<float[]> result = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            int header = 0;
            float firstSeconds = -1;
            while ((line = br.readLine()) != null) {
                if (header < 2)
                    header++;
                else {
                    try {
                        String[] comps = line.split("\t");
                        String timeStr = comps[1];
                        int lastWhitespace = 0;
                        if (comps[comps.length - 1].trim().length() == 0)
                            lastWhitespace = 1;
                        float angleX = Float.parseFloat(comps[comps.length - 3 - lastWhitespace]);
                        float angleY = Float.parseFloat(comps[comps.length - 2 - lastWhitespace]);
                        float angleZ = Float.parseFloat(comps[comps.length - 1 - lastWhitespace]);
                        Date date = new SimpleDateFormat("HH:mm:ss.SSS").parse(timeStr);
                        float seconds = date.getTime() / 1000.f;
                        if (firstSeconds == -1)
                            firstSeconds = seconds;
                        seconds -= firstSeconds;
                        result.add(new float[]{seconds, angleX, angleY, angleZ});
                    } catch (ParseException | NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private void makeChart(List<Entry> x, List<Entry> y, List<Entry> z, float xCalib, float yCalib, float zCalib) {
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        float minTime = x.get(0).getX();
        float maxTime = x.get(x.size() - 1).getX();

        LineDataSet setX;
        setX = new LineDataSet(x, "x");
        setX.setDrawIcons(false);
        setX.setColor(Color.RED);
        setX.setCircleColor(Color.RED);
        setX.setLineWidth(1f);
        setX.setCircleRadius(3f);
        setX.setDrawCircleHole(false);
        setX.setValueTextSize(9f);
        dataSets.add(setX);
        dataSets.add(getCalibLine("x calib", Color.rgb(0xe0, 0, 0), xCalib, minTime, maxTime));

        LineDataSet setY;
        setY = new LineDataSet(y, "y");
        setY.setDrawIcons(false);
        setY.setColor(Color.BLUE);
        setY.setCircleColor(Color.BLUE);
        setY.setLineWidth(1f);
        setY.setCircleRadius(3f);
        setY.setDrawCircleHole(false);
        setY.setValueTextSize(9f);
        dataSets.add(setY);
        dataSets.add(getCalibLine("y calib", Color.rgb(0, 0, 0xe0), yCalib, minTime, maxTime));

        LineDataSet setZ;
        setZ = new LineDataSet(z, "z");
        setZ.setDrawIcons(false);
        setZ.setColor(Color.GREEN);
        setZ.setCircleColor(Color.GREEN);
        setZ.setLineWidth(1f);
        setZ.setCircleRadius(3f);
        setZ.setDrawCircleHole(false);
        setZ.setValueTextSize(9f);
        dataSets.add(setZ);
        dataSets.add(getCalibLine("z calib", Color.rgb(0, 0xe0, 0), zCalib, minTime, maxTime));

        LineData data = new LineData(dataSets);
        mChart.setData(data);
    }

    private LineDataSet getCalibLine(String label, int color, float value, float minTime, float maxTime) {
        LineDataSet s;
        ArrayList<Entry> vals = new ArrayList<>();
        vals.add(new Entry(minTime, value));
        vals.add(new Entry(maxTime, value));
        s = new LineDataSet(vals, label);
        s.setDrawIcons(false);
        s.setColor(color);
        s.setCircleColor(color);
        s.setLineWidth(2f);
        s.setCircleRadius(3f);
        s.setDrawCircleHole(false);
        s.setValueTextSize(9f);
        return s;
    }

}
