package com.example.plexcam;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.plexcam.MainActivity;
import com.example.plexcam.R;

public class LoginActivity extends AppCompatActivity {
EditText emailid , password;
Button btnSignIn;
TextView tvSignUp;
FirebaseAuth mFirebaseAuth;
private FirebaseAuth.AuthStateListener mAuthStateListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login1);
        mFirebaseAuth=FirebaseAuth.getInstance();
        emailid=findViewById(R.id.emailId);
        password=findViewById(R.id.password);
        btnSignIn=findViewById(R.id.btnSignIn);
        tvSignUp=findViewById(R.id.tvSignUp);


        mAuthStateListener=new FirebaseAuth.AuthStateListener() {


            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser mFirebaseUser=mFirebaseAuth.getCurrentUser();
                if(mFirebaseUser!=null) {
                    Toast.makeText(LoginActivity.this, "Log in Successful", Toast.LENGTH_SHORT);
                    Intent i=new Intent(LoginActivity.this,MainActivity.class);
                    startActivity(i);
                }
                else{
                    Toast.makeText(LoginActivity.this, "Please try again", Toast.LENGTH_SHORT);
                }
            }
        };
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email=emailid.getText().toString();
                String pwd=password.getText().toString();
                if(email.isEmpty()){
                    emailid.setError("please enter email");
                    emailid.requestFocus();
                }
                else if (pwd.isEmpty()){
                    password.setError("please enter password");
                    password.requestFocus();

                }
                else if (email.isEmpty() && pwd.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Fields are empty ", Toast.LENGTH_SHORT).show();
                }
                else if (!(email.isEmpty() && pwd.isEmpty())) {
                    mFirebaseAuth.signInWithEmailAndPassword(email,pwd).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "Login Unsuccessful", Toast.LENGTH_SHORT).show();
                            } else {
                                Intent loginToMain = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(loginToMain);
                            }
                        }
                    });
                }
                     else{
                    Toast.makeText(LoginActivity.this, "An error Occurred ", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
       tvSignUp.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               Intent i=new Intent(LoginActivity.this,RegisterActivity.class);
               startActivity(i);
           }
       });


                }
            }

