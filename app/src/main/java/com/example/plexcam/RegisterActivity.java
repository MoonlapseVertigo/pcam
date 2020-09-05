package com.example.plexcam;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.plexcam.MainActivity;
import com.example.plexcam.R;
import  com.example.plexcam.LoginActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class RegisterActivity extends AppCompatActivity {
    EditText emailId,password;
    Button btnSignUp;
    TextView tvSignIn;
    FirebaseAuth mFirebaseAuth;
    FirebaseFirestore fStore;
    String userID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mFirebaseAuth=FirebaseAuth.getInstance();
        fStore=FirebaseFirestore.getInstance();
        emailId=findViewById(R.id.emailId);
         password=findViewById(R.id.password);
         tvSignIn=findViewById(R.id.tvSignIn);
         btnSignUp=findViewById(R.id.btnSignUp);
         btnSignUp.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 String email=emailId.getText().toString().trim();
                 String pwd=password.getText().toString().trim();
                 if(email.isEmpty()){
                     emailId.setError("please enter email");
                     emailId.requestFocus();
                 }
                 else if (pwd.isEmpty()){
                     password.setError("please enter password");
                     password.requestFocus();

                 }
                 else if (email.isEmpty() && pwd.isEmpty()) {
                     Toast.makeText(RegisterActivity.this, "Fields are empty ", Toast.LENGTH_SHORT).show();
                 }
                 else if (!(email.isEmpty() && pwd.isEmpty())) {
                     mFirebaseAuth.createUserWithEmailAndPassword(email,pwd).addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                         @Override
                         public void onComplete(@NonNull Task<AuthResult> task) {
                             if (!task.isSuccessful() ) {
                                 Toast.makeText(RegisterActivity.this, "Signup Unsuccessful", Toast.LENGTH_SHORT).show();
                             }
                             else{
                                 Toast.makeText(RegisterActivity.this, "User created successfully", Toast.LENGTH_SHORT).show();
                                 userID = mFirebaseAuth.getCurrentUser().getUid();
                                 DocumentReference documentReference= fStore.collection("users").document(userID);
                                 Map<String,Object> user=new HashMap<>();
                                 user.put("email",email);
                                 documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                     @Override
                                     public void onSuccess(Void aVoid) {
                                         Log.d("TAG","user profile is created"+userID);
                                     }
                                 });
                                 startActivity( new Intent(RegisterActivity.this,MainActivity.class));

                             }
                         }
                     });
                 }
                 else{
                     Toast.makeText(RegisterActivity.this, "An error Occurred , please try again ", Toast.LENGTH_SHORT).show();
                 }
             }
         });

tvSignIn.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        Intent regtolog=new Intent(RegisterActivity.this,LoginActivity.class);
        startActivity(regtolog);
    }
});
    }
}










