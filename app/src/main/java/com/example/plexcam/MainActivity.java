package com.example.plexcam;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;

import android.location.Location;
import android.widget.ViewFlipper;

import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceImageLabelerOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.stream.Collectors;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.Intent.ACTION_GET_CONTENT;
import static android.content.Intent.ACTION_INSERT_OR_EDIT;
import static android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE;
import static android.content.Intent.ACTION_PICK;
import static android.content.Intent.ACTION_VIEW;
import static android.provider.MediaStore.*;
import static android.provider.MediaStore.Images.Media.DEFAULT_SORT_ORDER;
import static android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;


public class MainActivity extends AppCompatActivity {
   
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    StorageReference mStorageRef;
    String userID;


    private static int counter = 1;
    private ImageView imageview;


    FusedLocationProviderClient fusedLocationProviderClient;
    TextView results;
    TextView userEmail;
    private TextView getLocation; //Add a new TextView to your activity_main to display the address
    HashSet<String> texts=new HashSet<>();
    private static final int REQUEST_IMAGE_CAPTURE = 101;

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private String[] REQUIRED_PERMISSIONS = new String[]{"Manifest.permission.CAMERA", "Manifest.permission.WRITE_EXTERNAL_STORAGE","Manifest.permission.READ.EXTERNAL.STORAGE" , "Manifest.permission.ACCESS_FINE_LOCATION","Manifest.permission.ACCESS_COARSE_LOCATION"};

    FirebaseVisionOnDeviceImageLabelerOptions options =
            new FirebaseVisionOnDeviceImageLabelerOptions.Builder()
                    .setConfidenceThreshold(0.5f)
                    .build();

    @Override
    protected void onStart()
    {
        super.onStart();
        Toast.makeText(getApplicationContext(),"Now onStart() calls", Toast.LENGTH_LONG).show(); //onStart Called
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "Permission is denied", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, REQUEST_CODE_PERMISSIONS);
        }


        }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageview = findViewById(R.id.imageView);
        getLocation = findViewById(R.id.getLocation);
        results = findViewById(R.id.results);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        userEmail=findViewById(R.id.userEmail);


            //Initializing some firestore variables for our storage
        fAuth=FirebaseAuth.getInstance();
        fStore=FirebaseFirestore.getInstance();
        mStorageRef=FirebaseStorage.getInstance().getReference();
        userID=fAuth.getCurrentUser().getUid();

                //the following code is for the users data collection in our cloud firestore
        DocumentReference documentReference=fStore.collection("users").document(userID);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                userEmail.setText(documentSnapshot.getString("email"));
            }
        });

          //The implementation of our method for fetching and labeling our last images
        Button btn=findViewById(R.id.galleryImport);


        btn.setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View view) {

                                       getLocation.setText("Not available for this function");
                                       results.setText("");

                                       FirebaseVisionImage image ;
                                       FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                                               .getOnDeviceImageLabeler(options);

                                       String path;
                                       results.append("Objects found in the 5 latest pictures");


                                       for ( int i = 0; i < 5; i++) {                                    //i just want to get the 5 latest images
                                           path = fetchGalleryImages(MainActivity.this).get(i); //getting the path from fetchGalleryImages arrayList method
                                           Bitmap bm=BitmapFactory.decodeFile(path);                    //creating bitmap from filepath
                                           image=FirebaseVisionImage.fromBitmap(bm);                    //FirebaseVisionImage from bitmap
                                           imageview.setImageBitmap(bm);

                                           labeler.processImage(image)
                                                   .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                                                       @Override
                                                       public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                                                           for (int j=0;j<labels.size();j++) {
                                                               String text= labels.get(j).getText();


                                                               texts.add(text);

                                                           }
                                                           results.setText("Objects found in your latest pictures taken");
                                                           results.append(texts.toString());
                                                       }


                                                   });
                                       }

                                   }

                               }
        );}

        public void uploadToFirebase(Uri uri) {
        if(uri!=null) {
            final StorageReference imageRef=mStorageRef.child("images/"+userID+"/"+uri.getLastPathSegment());
            UploadTask uploadTask=imageRef.putFile(uri);
            uploadTask.addOnFailureListener((e)->{
                Toast.makeText(this, "kai as poyme error", Toast.LENGTH_SHORT).show();
            });
        }

        }
    public Uri getImageUri(MainActivity inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }



    public ArrayList<String> fetchGalleryImages(Activity context) { //method to fetch Android Gallery images
        ArrayList<String> galleryImageUrls;
        final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID};//get all columns of type images
        final String orderBy = MediaStore.Images.Media.DATE_TAKEN;//order data by date

        Cursor imagecursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null,
                null, orderBy + " DESC");          //get all data in Cursor by sorting in DESC order

        galleryImageUrls = new ArrayList<String>();

        for (int i = 0; i < imagecursor.getCount(); i++) {
            imagecursor.moveToPosition(i);
            int dataColumnIndex = imagecursor.getColumnIndex(MediaStore.Images.Media.DATA);//get column index
            galleryImageUrls.add(imagecursor.getString(dataColumnIndex));//get Image from column index

        }
        Log.e("gettin'","images");
        return galleryImageUrls;
    }




    //Our method for getting users location
    private void getlastlocation() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "Permission is denied", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{ACCESS_COARSE_LOCATION,ACCESS_FINE_LOCATION},REQUEST_CODE_PERMISSIONS);
        }



        Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    double longi = location.getLongitude();
                    double lati = location.getLatitude();
                    Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(lati, longi, 1);
                        getLocation.setText("Location : " + addresses.get(0).getCountryCode() + ", " + addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }
            }
        });
    }

   //Don't even know if its useful anymore after many changes
    // but my program works , so i'll just err on the side of caution
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    public void ShootAndDetect(View view) {   //method one : taking shot from in-app camera and recognize objects automatically
        Intent imageTakeIntent = new Intent(ACTION_IMAGE_CAPTURE);
        if (imageTakeIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(imageTakeIntent, REQUEST_IMAGE_CAPTURE);
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturned) {


        super.onActivityResult(requestCode, resultCode, imageReturned);

                if (resultCode !=RESULT_CANCELED) {
                    if(requestCode==101) {
                    counter++;



                    Bundle extras = imageReturned.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    imageview.setImageBitmap(imageBitmap);
                    results.setText("");
                    Images.Media.insertImage(getContentResolver(), imageBitmap, "shot" + counter, "description");   //Saving Image to Gallery

                        uploadToFirebase(  getImageUri(this, imageBitmap));      //getting the Uri of our bitmap via getImageUri method and Uploading to User's storage folder


                    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(imageBitmap);
                    FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                            .getOnDeviceImageLabeler(options);
                    labeler.processImage(image)
                            .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                                @Override
                                public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                                    for (FirebaseVisionImageLabel label : labels) {
                                        String text = label.getText();
                                        float confidence = label.getConfidence();
                                        results.append("\n"+text+" : " + confidence);

                                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                            return;
                                        }
                                        getlastlocation();


                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MainActivity.this, "Task Failed Successfully", Toast.LENGTH_SHORT).show();
                                }
                            });

                }


        }


    }}





