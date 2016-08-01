package ru.minlexx.xnovaalarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import java.util.Locale;

import ru.minlexx.xnovaalarm.ifaces.IMainActivity;


public class LoginFormFragment extends Fragment {
    private static final String TAG = LoginFormFragment.class.getName();

    private static final String PREFS_AUTH_FILENAME = "auth";
    private static final String PREFS_LOGIN = "xn_login";
    private static final String PREFS_PASS = "xn_pass";
    private static final String PREFS_REMEMBER = "xn_remember";

    // GUI controls
    private CheckBox cb_remember = null;
    private EditText et_login = null;
    private EditText et_pass = null;
    private Button btn_login = null;


    IMainActivity m_mainActivity = null;


    public LoginFormFragment() {
        // Required empty public constructor
    }

    public static LoginFormFragment newInstance() {
        return new LoginFormFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_login_form, container, false);
        // get GUI controls
        cb_remember = (CheckBox)fragmentView.findViewById(R.id.cb_remember);
        et_login = (EditText)fragmentView.findViewById(R.id.et_xnovalogin);
        et_pass = (EditText)fragmentView.findViewById(R.id.et_xnovapassword);
        btn_login = (Button)fragmentView.findViewById(R.id.button_login);
        //
        assert(cb_remember != null);
        assert(et_login != null);
        assert(et_pass != null);
        assert(btn_login != null);
        //
        this.doRestoreInstanceState(savedInstanceState);
        //
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginFormFragment.this.onButtonLoginPressed(v);
            }
        });
        //
        return fragmentView;
    }

    public void onButtonLoginPressed(View view) {
        if (m_mainActivity == null) return;
        RefresherService srv = m_mainActivity.getRefresherService();
        if (srv != null) {
            String s_login = et_login.getText().toString();
            String s_pass = et_pass.getText().toString();
            Log.d(TAG, String.format(Locale.getDefault(),
                    "Clicked login: %s / %s", s_login, s_pass));
            srv.beginXNovaLogin(s_login, s_pass);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IMainActivity) {
            m_mainActivity = (IMainActivity)context;
        } else {
            throw new RuntimeException(context.toString() + " must implement IMainActivity");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        m_mainActivity = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState()");
        //
        outState.putBoolean("cb_remember", cb_remember.isChecked());
        outState.putString("login", et_login.getText().toString());
        outState.putString("pass", et_pass.getText().toString());
    }

    protected void doRestoreInstanceState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) return;
        Log.d(TAG, "doRestoreInstanceState()");
        //
        cb_remember.setChecked(savedInstanceState.getBoolean("cb_remember"));
        et_login.setText(savedInstanceState.getString("login"));
        et_pass.setText(savedInstanceState.getString("pass"));
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences prefs = getActivity().getSharedPreferences(
                PREFS_AUTH_FILENAME, Context.MODE_PRIVATE);
        String saved_login = prefs.getString(PREFS_LOGIN, "");
        String saved_pass = prefs.getString(PREFS_PASS, "");
        boolean saved_remember = prefs.getBoolean(PREFS_REMEMBER, false);
        // restore account data if "remember" was checked
        if (saved_remember) {
            cb_remember.setChecked(true);
            et_login.setText(saved_login);
            et_pass.setText(saved_pass);
            Log.d(TAG, "onStart(): loaded savedata");
        } else {
            cb_remember.setChecked(false);
            et_login.setText("");
            et_pass.setText("");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // save auth form data?
        SharedPreferences prefs = getActivity().getSharedPreferences(
                PREFS_AUTH_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs_editor = prefs.edit();
        if (cb_remember.isChecked()) {
            prefs_editor.putBoolean(PREFS_REMEMBER, true);
            prefs_editor.putString(PREFS_LOGIN, et_login.getText().toString());
            prefs_editor.putString(PREFS_PASS, et_pass.getText().toString());
            Log.d(TAG, "onStop(): saved savedata");
        } else {
            prefs_editor.putBoolean(PREFS_REMEMBER, false);
            prefs_editor.putString(PREFS_LOGIN, "");
            prefs_editor.putString(PREFS_PASS, "");
        }
        prefs_editor.apply();
    }

    public void updateButtonsEnabledStates(boolean isAuthed) {
        if (isAuthed) {
            et_login.setEnabled(false);
            et_pass.setEnabled(false);
            cb_remember.setEnabled(false);
            btn_login.setEnabled(false);
        } else {
            et_login.setEnabled(true);
            et_pass.setEnabled(true);
            cb_remember.setEnabled(true);
            btn_login.setEnabled(true);
        }
    }
}
