package com.example.lukas.androidbluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TerminalFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TerminalFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TerminalFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private View mFragmentView;

    private int ctr = 0;
    private OnFragmentInteractionListener mListener;

    private TextView terminal;
    SharedPreferences sharedPref;
    private boolean isLogging = false;

    static TerminalFragment fragment;
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TerminalFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TerminalFragment newInstance(String param1, String param2) {
        fragment = new TerminalFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public TerminalFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        sharedPref =  PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mFragmentView = inflater.inflate(R.layout.fragment_terminal, container, false);
        terminal = (TextView)mFragmentView.findViewById(R.id.terminalTextView);
        final Button button = (Button)mFragmentView.findViewById(R.id.logButton);
        final Button sendButton = (Button)mFragmentView.findViewById(R.id.sendButton);
        if(isLogging){
            button.setText("Stop Logging");
        }else{
            button.setText("Start Logging");
        }
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LinearLayout layout = new LinearLayout(getActivity());
                layout.setOrientation(LinearLayout.VERTICAL);
                builder.setTitle("Enter Message");
                // Set up the input
                final EditText input = new EditText(getActivity());
                final CheckBox checkbox = new CheckBox(getActivity());
                checkbox.setText("Send line endings");
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                layout.addView(input);
                layout.addView(checkbox);
                builder.setView(layout);
                builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //start file logging
                        String message = input.getText().toString();
                        Boolean lineEndings = checkbox.isChecked();
                        ((MainActivity)getActivity()).sendMessage(message,lineEndings);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!isLogging) {
                    //check for timestamp settings
                    final boolean timestampOn = sharedPref.getBoolean("pref_autoNaming", false);
                    final boolean append = sharedPref.getBoolean("pref_timestampAdditionalName", false);
                    if(timestampOn && !append){
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
                        String currentTimeStamp = dateFormat.format(new Date());
                        ((MainActivity) getActivity()).initFileWrite(currentTimeStamp+".txt");
                        isLogging = !isLogging;
                        button.setText("Stop Logging");
                    }else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle("Enter Filename");
                        // Set up the input
                        final EditText input = new EditText(getActivity());
                        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        builder.setView(input);
                        // Set up the buttons
                        builder.setPositiveButton("Start", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //start file logging
                                String fileName = input.getText().toString();
                                if(append){
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
                                    String currentTimeStamp = dateFormat.format(new Date());
                                    fileName += currentTimeStamp;
                                }
                                ((MainActivity) getActivity()).initFileWrite(fileName + ".txt");
                                isLogging = !isLogging;
                                button.setText("Stop Logging");
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        builder.show();
                    }
                }else{
                    ((MainActivity)getActivity()).stopFileWrite();
                    button.setText("Start Logging");
                    isLogging = !isLogging;
                    Toast toast = Toast.makeText(getActivity().getApplicationContext(), "The logfile is located in your external root folder.", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
        return mFragmentView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    public void updateText(String text){
        final ScrollView mScrollView = (ScrollView)mFragmentView.findViewById(R.id.SCROLLER_ID);
        int tmp = Integer.parseInt(sharedPref.getString("pref_lastX", "400"));
        if(ctr >= tmp){
            String t = terminal.getText().toString();
            if(t.length()>=tmp) {
                terminal.setText(t.substring(t.length() - (tmp/2)));
            }
            ctr = 0;
        }
        terminal.append(text+"\n");
        mScrollView.post(new Runnable()
        {
            public void run()
            {
                mScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
        ctr++;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
