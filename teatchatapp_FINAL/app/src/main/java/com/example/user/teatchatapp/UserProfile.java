package com.example.user.teatchatapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
//import android.widget.Toolbar;
import android.support.v7.widget.Toolbar;

import com.example.user.teatchatapp.Common.Common;
import com.example.user.teatchatapp.Holder.QBUsersHolder;
import com.quickblox.chat.QBChatService;
import com.quickblox.content.QBContent;
import com.quickblox.content.model.QBFile;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class UserProfile extends AppCompatActivity {

    EditText edtPassword,edtOldPassword,edtFullName,edtEmail,edtPhone;
    Button btnUpdate,btnCancel;

    //Part 11 set user avatar
    ImageView user_avatar;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.user_update_log_out:
                logOut();
                break;
            default:
                break;
        }
        return true;
    }

    private void logOut() {
        QBUsers.signOut().performAsync(new QBEntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                QBChatService.getInstance().logout(new QBEntityCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid, Bundle bundle) {
                        Toast.makeText(UserProfile.this, "You are logout !!!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(UserProfile.this,MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);// remove all previous activity
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(QBResponseException e) {

                    }
                });
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_update_menu,menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        //Add Toolbar
        Toolbar toolbar = (Toolbar)findViewById(R.id.user_update_toolbar);
        toolbar.setTitle("TeatChat beta");
        setSupportActionBar(toolbar); //影片12:55

        initView();

        loadUserProfile();

        //Part 11
        user_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"),Common.SELECT_PICTURE);
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {//更新使用者資料
            @Override
            public void onClick(View v) {
                String password = edtPassword.getText().toString();
                String oldPasswod = edtOldPassword.getText().toString();
                String email = edtEmail.getText().toString();
                String phone = edtPhone.getText().toString();
                String fullname = edtFullName.getText().toString();

                QBUser user = new QBUser();
                user.setId(QBChatService.getInstance().getUser().getId());
                if(!Common.isNullOrEmptyString(oldPasswod));//判斷選項不是空白才更新
                    user.setOldPassword(oldPasswod);
                if(!Common.isNullOrEmptyString(password));
                    user.setPassword(password);
                if(!Common.isNullOrEmptyString(fullname));
                    user.setFullName(fullname);
                if(!Common.isNullOrEmptyString(email));
                    user.setEmail(email);
                if(!Common.isNullOrEmptyString(phone));
                    user.setPhone(phone);

                ProgressBar progressBar,progressBar_cyclic;
                //mDialog.show();
                QBUsers.updateUser(user).performAsync(new QBEntityCallback<QBUser>() {
                    @Override
                    public void onSuccess(QBUser qbUser, Bundle bundle) {
                        Toast.makeText(UserProfile.this, "User: "+qbUser.getLogin()+" updated",Toast.LENGTH_SHORT).show();
                        //mDialog.dimiss();
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Toast.makeText(UserProfile.this, "ERROR"+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK){
            if(requestCode == Common.SELECT_PICTURE){
                Uri selectedImageUri = data.getData();

                //final ProgressBar

                //Update user  avatar (we will read stream from file and upload it int Server)
                try{
                    InputStream in = getContentResolver().openInputStream(selectedImageUri);
                    final Bitmap bitmap = BitmapFactory.decodeStream(in);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,bos);
                    File file = new File(Environment.getExternalStorageDirectory()+"/myimage.png");
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(bos.toByteArray());
                    fos.flush();
                    fos.close();

                    //Get file size
                    final int imageSizeKb = (int)file.length() / 1024;
                    if(imageSizeKb >= (1024*100)){
                        Toast.makeText(this, "Error image size", Toast.LENGTH_SHORT).show();
                    }

                    //Upload file to server
                    QBContent.uploadFileTask(file,true,null)
                            .performAsync(new QBEntityCallback<QBFile>() {
                                @Override
                                public void onSuccess(QBFile qbFile, Bundle bundle) {
                                    //Set avatar for user
                                    QBUser user = new QBUser();
                                    user.setId(QBChatService.getInstance().getUser().getId());
                                    user.setFileId(Integer.parseInt(qbFile.getId().toString()));

                                    //Update user
                                    QBUsers.updateUser(user)
                                            .performAsync(new QBEntityCallback<QBUser>() {
                                                @Override
                                                public void onSuccess(QBUser user, Bundle bundle) {
                                                    //mDialog.dismiss();
                                                    user_avatar.setImageBitmap(bitmap);
                                                }

                                                @Override
                                                public void onError(QBResponseException e) {

                                                }
                                            });
                                }

                                @Override
                                public void onError(QBResponseException e) {

                                }
                            });
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void loadUserProfile() {

        //Part 11 Load avatar (load user information from Server and update image view)
        QBUsers.getUser(QBChatService.getInstance().getUser().getId())
                .performAsync(new QBEntityCallback<QBUser>() {
                    @Override
                    public void onSuccess(QBUser qbUser, Bundle bundle) {
                        //save to cache (Don't forgey to cache this user)
                        QBUsersHolder.getInstance().putUser(qbUser);
                        if (qbUser.getFileId() != null){
                            int profilePictureId = qbUser.getFileId();

                            QBContent.getFile(profilePictureId)
                                    .performAsync(new QBEntityCallback<QBFile>() {
                                        @Override
                                        public void onSuccess(QBFile qbFile, Bundle bundle) {
                                            //After get file success, just show it in ImageView
                                            String fileUrl = qbFile.getPublicUrl();
                                            Picasso.with(getBaseContext())
                                                    .load(fileUrl)
                                                    .into(user_avatar);
                                        }

                                        @Override
                                        public void onError(QBResponseException e) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onError(QBResponseException e) {

                    }
                });

        QBUser currentUser = QBChatService.getInstance().getUser();
        String fullname = currentUser.getFullName();
        String email = currentUser.getEmail();
        String phone = currentUser.getPhone();

        edtEmail.setText(email);
        edtFullName.setText(fullname);
        edtPhone.setText(phone);
    }

    private void initView() {
        btnCancel = (Button)findViewById(R.id.update_user_btn_cancel);
        btnUpdate = (Button)findViewById(R.id.update_user_btn_update);

        edtEmail = (EditText)findViewById(R.id.update_edt_email);
        edtPhone = (EditText)findViewById(R.id.update_edt_phone);
        edtFullName = (EditText)findViewById(R.id.update_edt_full_name);
        edtPassword = (EditText)findViewById(R.id.update_edt_password);
        edtOldPassword = (EditText)findViewById(R.id.update_edt_old_password);

        //Part 11
        user_avatar = (ImageView)findViewById(R.id.user_avatar);
    }
}
