package com.example.myapplication;

import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Intent;

import android.net.Uri;
import android.provider.MediaStore;

import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Vladimir Cvjetkovic
 */
public class MainActivity extends Activity implements InputDialog.InputDialogListener {

    private DrawView drawView;
    private FrameLayout preview;
    private Button btn_ok;
    private Button btn_cancel;
    private Button btn_takePicture;
    private File photoFile;
    private double result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        preview = (FrameLayout) findViewById(R.id.camera_preview);

        btn_takePicture = (Button) findViewById(R.id.button_takePicture);
        btn_ok = (Button) findViewById(R.id.button_calculate);
        btn_cancel = (Button) findViewById(R.id.button_cancel);

        addPictureButton();

        btn_takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new InputDialog().show(getFragmentManager(), "input_dialog");
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.clearCanvas();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
//            case R.id.action_settings:
//                break;
            case R.id.action_cleanStorage:
                cleanPhotoStorage();
                break;
            case R.id.action_choosePhoto:
                dispatchChoosePhotoIntent();
                break;
            case R.id.action_takePicture:
                dispatchTakePictureIntent();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image", Toast.LENGTH_SHORT);
            }
            if (photoFile != null) {
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void addPictureButton() {
        preview.removeAllViews();
        btn_cancel.setVisibility(View.GONE);
        btn_ok.setVisibility(View.GONE);
        btn_takePicture.setVisibility(View.VISIBLE);
    }

    private void pictureTaken() {
        preview.removeAllViews();
        btn_cancel.setVisibility(View.VISIBLE);
        btn_ok.setVisibility(View.VISIBLE);
        btn_takePicture.setVisibility(View.GONE);

        ImageSurface image = new ImageSurface(this, photoFile);
        preview.addView(image);

        ((TextView) findViewById(R.id.info_lbl)).setText(getResources().getString(R.string.setReferencePoints));

        drawView = new DrawView(this);
        preview.addView(drawView);
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_SELECT_PHOTO = 2;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK)
                    pictureTaken();
                break;
            case REQUEST_SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    String filePath = Utils.getPath(this, uri);
                    photoFile = new File(filePath);
                    pictureTaken();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }

    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());

        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);

        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    private void cleanPhotoStorage() {
        File storageDir = getExternalFilesDir(null);
        File fList[] = storageDir.listFiles();
        //Search for pictures in the directory
        for (int i = 0; i < fList.length; i++) {
            String pes = fList[i].getName();
            if (pes.endsWith(".jpg"))
                new File(fList[i].getAbsolutePath()).delete();
        }
        Toast.makeText(this, getResources().getString(R.string.storageDeleted), Toast.LENGTH_SHORT).show();
    }

    private void dispatchChoosePhotoIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.action_choosePhoto)), REQUEST_SELECT_PHOTO);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        int inputUnit = ((Spinner) dialog.getDialog().findViewById(R.id.input_unit_chooser)).getSelectedItemPosition();
        int outputUnit = ((Spinner) dialog.getDialog().findViewById(R.id.output_unit_chooser)).getSelectedItemPosition();
        try {
            double reference = Double.parseDouble(((EditText) dialog.getDialog().findViewById(R.id.reference_input)).getText().toString());
            result = drawView.calculate(reference, inputUnit, outputUnit);
            showResult();
        } catch (NumberFormatException ex) {
            Toast.makeText(this, getResources().getString(R.string.error_numberFormat), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
    }

    private void showResult() {
        if (result != -1) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getResources().getString(R.string.result_lbl) + decimalFormat.format(result));
            builder.create().show();
        }
    }
}