package com.example.user.teatchatapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.quickblox.auth.session.QBSettings;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.QBSettingsSaver;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

public class MainActivity extends AppCompatActivity {

    static final String APP_ID = "69865";
    static final String AUTH_KEY = "zNgECMUd-HHjyPT";
    static final String AUTH_SECRET = "HSQ6T6HbMkUzUAO";
    static final String ACCOUNT_KEY = "TbXXkgw9WMZ2yYyCeYa_";

    //Part 10
    static final int REQUEST_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        requestRuntimePermission();

        initializeFramwork();

        Button btnLogin = findViewById(R.id.main_btnLogin);
        Button btnSignup = findViewById(R.id.main_btnSignup);

        final EditText edtPassword = findViewById(R.id.main_editPassword);
        final EditText edtUser = findViewById(R.id.main_editLogin);

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,SignUpActivity.class));
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String user = edtUser.getText().toString();
                final String password = edtPassword.getText().toString();

                QBUser qbUser = new QBUser(user,password);
                QBUsers.signIn(qbUser).performAsync(new QBEntityCallback<QBUser>() {
                    @Override
                    public void onSuccess(QBUser qbUser, Bundle bundle) {
                        Toast.makeText(getBaseContext(),"Login successfully",Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(MainActivity.this,ChatDialogsActivity.class);
                        intent.putExtra("user",user);
                        intent.putExtra("password",password);
                        startActivity(intent);
                        finish(); // Close login activity after logged
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Toast.makeText(getBaseContext(),""+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void requestRuntimePermission() {
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
            },REQUEST_CODE);
        }
    }//Part 10

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_CODE:{
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(getBaseContext(),"Permission Granted",Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getBaseContext(),"Permission Denied",Toast.LENGTH_SHORT).show();
            }
            break;
        }
    } //Part 10

    private void initializeFramwork() {
        QBSettings.getInstance().init(getApplicationContext(),APP_ID,AUTH_KEY,AUTH_SECRET);
        QBSettings.getInstance().setAccountKey(ACCOUNT_KEY);
    }
}
