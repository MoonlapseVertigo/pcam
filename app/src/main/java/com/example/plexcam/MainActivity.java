package com.example.plexcam;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.solver.widgets.Snapshot;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;

import android.location.Location;

import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceImageLabelerOptions;
import com.google.firebase.storage.FirebaseStorage;

import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;


import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.provider.MediaStore.*;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
 // We are implementing multiple Firebase services , Authentication System,Firestore ,Firebase Storage
    //and Firebase Realtime Database , each one for different purpose.

    FirebaseAuth fAuth; //firebase authentication service
    FirebaseFirestore fStore; //firebase firestore object
    StorageReference mStorageRef; //firebase Storage object
    FirebaseDatabase database=FirebaseDatabase.getInstance(); //Firebase RealTime Database object

    //we need this as global variable
    LatLng latLong;

    String userID;
    StringBuilder userEmail;

    //to be saved as metadata on each image
    public static String myLocation;
    public static StringBuilder fbLabels;
    public double longi, lati; // we gonna use those in more than one method so we make them class variables.

    ArrayList snaplist;



    private static int counter = 1;

    private ImageView imageview; //the image that is been showed on our Main activity
    private TextView UserEmail; //users email that is been showed on top right of our Main activity


    FusedLocationProviderClient fusedLocationProviderClient; // the High Level api we are using for getting our current location
    TextView results;  //Our text view that shows the labels that are being detected


    private TextView getLocation; //Add a new TextView to your activity_main to display the address
    HashSet<String> texts = new HashSet<>(); //HashSet to prevent duplicates xd
    private static final int REQUEST_IMAGE_CAPTURE = 101;

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private String[] REQUIRED_PERMISSIONS = new String[]{"Manifest.permission.CAMERA",
            "Manifest.permission.WRITE_EXTERNAL_STORAGE", "Manifest.permission.READ.EXTERNAL.STORAGE",
            "Manifest.permission.ACCESS_FINE_LOCATION", "Manifest.permission.ACCESS_COARSE_LOCATION"};

    FirebaseVisionOnDeviceImageLabelerOptions options =
            new FirebaseVisionOnDeviceImageLabelerOptions.Builder()
                    .setConfidenceThreshold(0.5f)
                    .build();

    @Override
    protected void onStart() {
        super.onStart();
        setprofile();

        Toast.makeText(getApplicationContext(), "App is running", Toast.LENGTH_LONG).show(); //onStart Called
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS);
        }


    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Initializing some textviews and imageview
        UserEmail=findViewById(R.id.userEmail);
        imageview = findViewById(R.id.imageView);
        getLocation = findViewById(R.id.getLocation);
        results = findViewById(R.id.results);
        fStore=FirebaseFirestore.getInstance();


        //Initialize our google API for finding our current location.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);



        results.setText(""); //Resetting our textview on each creation

       //Creating a SupportMapFragment Object
        //basically a fragment for our map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        //Initializing some firestore variables for our storage
        myLocation = new String();
        fbLabels = new StringBuilder();
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        userID = fAuth.getCurrentUser().getUid();
        userEmail = new StringBuilder();
        userEmail.append(fAuth.getCurrentUser().getEmail());
        UserEmail.setText(userEmail);//setting our textview on our top-right side of our screen
        //the following code is for the users data collection in our cloud firestore
        DocumentReference documentReference = fStore.collection("users").document(userID);




        //The implementation of our method for fetching and labeling our last images

        Button btn = findViewById(R.id.galleryImport);
        btn.setOnClickListener(view -> {

                    getLocation.setText("Not available for this function");//We return this message cause location is not important for this method
                    results.setText("");

                    FirebaseVisionImage image;
                    FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                            .getOnDeviceImageLabeler(options);

                    String path;
                    results.append("Objects found in the 5 latest pictures");
             try {

                 for (int i = 0; i < 5; i++) {                                    //i just want to get the 5 latest images
                     path = fetchGalleryImages(MainActivity.this).get(i); //getting the path from fetchGalleryImages arrayList method
                     Bitmap bm = BitmapFactory.decodeFile(path);                    //creating bitmap from filepath
                     image = FirebaseVisionImage.fromBitmap(bm);                    //FirebaseVisionImage from bitmap
                     imageview.setImageBitmap(bm);

                     labeler.processImage(image)
                             .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                                 @Override
                                 public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                                     for (int j = 0; j < labels.size(); j++) {
                                         String text = labels.get(j).getText();


                                         texts.add(text);

                                     }
                                     results.setText("Objects found in your latest pictures taken");
                                     results.append(texts.toString());
                                 }


                             });
                 }

             }
             catch (Exception e) {
                 e.printStackTrace();
             }

                }
        );
    }

   public void setprofile(){
        snaplist=new ArrayList<Snapshot>();
      // DatabaseReference myRef=database.getReference().child("Location").child(userID);
        DatabaseReference myRef2=database.getReference().child("Labels").child(userID);
        myRef2.addListenerForSingleValueEvent(new ValueEventListener() {
            //Firebase services are all about EventListeners
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //we are using the onDataChange to retrieve all the LatLongs(Locations)
                //that user saved in firebase realtime Database
                Iterable<DataSnapshot> snapshotIterable=snapshot.getChildren();
                //we iterate the collection of LatLongs
                //and each time we pass the values to a new LatLng object
                //which we put as parameter to our marker
                //this way we get all the user's marks and we put them to our google map.


                for (DataSnapshot dataSnapshot: snapshotIterable) {

                    snaplist.add(dataSnapshot.getValue());


                }
                Map<String, Long> map = (Map<String, Long>) snaplist.stream()
                        .collect(Collectors.groupingBy(w -> w, Collectors.counting()));
                List<Map.Entry<String, Long>> result = map.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .limit(3)  //to return the 3 most favourite items'

                        .collect(Collectors.toList());


                UserEmail.setText(userEmail+"\n"+"Favorite labels"+"\n"+result.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        } );

    }

//our method for pushing our file with its metadata on firebase storage.
    public void uploadToFirebase(Uri uri) {
        if (uri != null) {
            final StorageReference imageRef = mStorageRef.child("images/" + userID + "/" + uri.getLastPathSegment());


            //Adding metadata to our pictures
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setCustomMetadata("UserID", userID)        //the unique user ID
                    .setCustomMetadata("Location", myLocation)  //location of the shot
                    .setCustomMetadata("labels:", String.valueOf(fbLabels)) //labels that have been detected
                    .build();




            UploadTask uploadTask = imageRef.putFile(uri, metadata); //The upload task
            uploadTask.addOnFailureListener((e) -> {
                Toast.makeText(this, "Uploading task failed", Toast.LENGTH_SHORT).show();
            });
        }

    }







    //In order to upload an image to firebase we need to find its uri.That's what this method does.
    public Uri getImageUri(MainActivity inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }




     //This method is for fetching the last images from the user's gallery
    public ArrayList<String> fetchGalleryImages(Activity context) {

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
        Log.e("getting'", "images");
        return galleryImageUrls;
    }


    //Our method for getting users location by using the google's high level API FusedLocationProvider
    public void getlastlocation() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "Permission is denied", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, REQUEST_CODE_PERMISSIONS);
        }


        Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
        locationTask.addOnSuccessListener(location -> {
            if (location != null) {
                longi = location.getLongitude();
                lati = location.getLatitude();
              latLong=new LatLng(lati,longi); //adding our lat,long in a LatLong object
                //We also need to initiate a geoCoder which converts our coordinates to actual readable Location.
                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(lati, longi, 1);
                    getLocation.setText("Location : " + addresses.get(0).getCountryCode() + ", " + addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea());

                    myLocation = addresses.get(0).getCountryName() + " , " + addresses.get(0).getAdminArea();  // this passes the correct value to be used for the uploading process
                    //Here we are saving the coordinates in our Firebase Realtime Database
                    DatabaseReference myRef=database.getReference("Location/"+userID+"/"); //creating path reference
                    DatabaseReference pushref=myRef.push(); //Creating push reference
                    pushref.setValue(latLong); //pushing the value
                } catch (IOException e) {
                    e.printStackTrace();
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

    //method one : taking shot from in-app camera and recognize objects automatically
    public void ShootAndDetect(View view) {
        Intent imageTakeIntent = new Intent(ACTION_IMAGE_CAPTURE); //This is the intent to open the camera.
        if (imageTakeIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(imageTakeIntent, REQUEST_IMAGE_CAPTURE);
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }



    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturned) {


        super.onActivityResult(requestCode, resultCode, imageReturned);

        if (resultCode != RESULT_CANCELED) {
            if (requestCode == 101) {

               //We are gonna use Bitmap as image type for increased performance.
                Bundle extras = imageReturned.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                imageview.setImageBitmap(imageBitmap);
                results.setText("");
                Images.Media.insertImage(getContentResolver(), imageBitmap, "shot" + counter, "description");   //Saving Image to Gallery
                getlastlocation();  //Right after the pic is taken we need to get our location

                   //Standard implementation of the Firebase on Device Labeler
                FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(imageBitmap);
                FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                        .getOnDeviceImageLabeler(options);
                labeler.processImage(image)  //passing as parameter to processImage method our image
                        .addOnSuccessListener(labels -> {
                            //for every label detected in our picture we append it to our text view.
                            for (FirebaseVisionImageLabel label : labels) {
                                String text = label.getText();
                                float confidence = label.getConfidence();
                                results.append("\n" + text + " : " + confidence); //Adding label followed by confidence .
                                fbLabels.append(text + ",");                      //Also adding our label in fbLabels list
                                DatabaseReference myRef2=database.getReference("Labels/"+userID+"/");                                                //to be saved as metadata .
                                DatabaseReference pushref=myRef2.push(); //Creating push reference
                                pushref.setValue(label.getText()); //pushing the values
                            }


                            uploadToFirebase(getImageUri(MainActivity.this, imageBitmap)); //Sending our objects to our CloudStore

                        })
                        .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Task Failed Successfully", Toast.LENGTH_SHORT).show());


            }


        }


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
           //Checking for permissions is Mandatory,else we get an error.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
MarkerOptions marker=new MarkerOptions(); //Creating a MarkerOptions Object in which we later pass our coordinates.

        googleMap.setMyLocationEnabled(true);

        DatabaseReference myRef=database.getReference().child("Location").child(userID); //creating reference
             myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                 //Firebase services are all about EventListeners
                 @Override
                 public void onDataChange(@NonNull DataSnapshot snapshot) {
                     //we are using the onDataChange to retrieve all the LatLongs(Locations)
                     //that user saved in firebase realtime Database
                     Iterable<DataSnapshot> snapshotIterable=snapshot.getChildren();
                     //we iterate the collection of LatLongs
                     //and each time we pass the values to a new LatLng object
                     //which we put as parameter to our marker
                     //this way we get all the user's marks and we put them to our google map.
                     for (DataSnapshot dataSnapshot: snapshotIterable) {
                      double lat= (double) dataSnapshot.child("latitude").getValue();
                         double lng= (double) dataSnapshot.child("longitude").getValue();
                     LatLng ltlng=new LatLng(lat,lng);
                    googleMap.addMarker(new MarkerOptions().position(ltlng));

                     }

                 }

                 @Override
                 public void onCancelled(@NonNull DatabaseError error) {

                 }
             } );







    }
}





