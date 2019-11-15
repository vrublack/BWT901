package com.example;


import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DataFragment extends Fragment{
	 @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	    }
	    
	    @Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container,
	            Bundle savedInstanceState) {
	        View view = inflater.inflate(R.layout.fragment_data, null);
	        return view;
	    }
	    
	    @Override
	    public void onPause() {
	        super.onPause();
	    }
}
