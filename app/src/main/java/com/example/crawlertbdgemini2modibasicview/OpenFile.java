package com.example.crawlertbdgemini2modibasicview;
//https://www.androidsnippets.com/open-any-type-of-file-with-default-intent.html
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

//To open file:
//        File myFile = new File("your any type of file url");
//        FileOpen.openFile(mContext, myFile);
public class OpenFile {
    public static void openFile(Context context, File file) {
        // Create URI
        //File file=url;
        Uri uri = Uri.fromFile(file);    //file
        Intent intent = new Intent(Intent.ACTION_VIEW);
        // Check what kind of file you are trying to open, by comparing the url with extensions.
        // When the if condition is matched, plugin sets the correct intent (mime) type,
        // so Android knew what application to use to open the file
        if (file.toString().contains(".doc") || file.toString().contains(".docx")) {
            // Word document
            intent.setDataAndType(uri, "application/msword");
        } else if(file.toString().contains(".pdf")) {
            // PDF file
            intent.setDataAndType(uri, "application/pdf");
        } else if(file.toString().contains(".ppt") || file.toString().contains(".pptx")) {
            // Powerpoint file
            intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
        } else if(file.toString().contains(".xls") || file.toString().contains(".xlsx")) {
            // Excel file
            intent.setDataAndType(uri, "application/vnd.ms-excel");
        } else if(file.toString().contains(".zip") || file.toString().contains(".rar")) {
            // WAV audio file
            intent.setDataAndType(uri, "application/x-wav");
        } else if(file.toString().contains(".rtf")) {
            // RTF file
            intent.setDataAndType(uri, "application/rtf");
        } else if(file.toString().contains(".wav") || file.toString().contains(".mp3")) {
            // WAV audio file
            intent.setDataAndType(uri, "audio/x-wav");
        } else if(file.toString().contains(".gif")) {
            // GIF file
            intent.setDataAndType(uri, "image/gif");
        } else if(file.toString().contains(".jpg") || file.toString().contains(".jpeg") || file.toString().contains(".png")) {
            // JPG file
            intent.setDataAndType(uri, "image/jpeg");
        } else if(file.toString().contains(".txt")) {
            // Text file
            intent.setDataAndType(uri, "text/plain");
        } else if(file.toString().contains(".3gp") || file.toString().contains(".mpg")
                  || file.toString().contains(".mpeg") || file.toString().contains(".mpe")
                  || file.toString().contains(".mp4") || file.toString().contains(".avi")) {
            // Video files
            intent.setDataAndType(uri, "video/*");
        } else {
            //if you want you can also define the intent type for any other file

            //additionally use else clause below, to manage other unknown extensions
            //in this case, Android will show all applications installed on the device
            //so you can choose which application to use
            intent.setDataAndType(uri, "*/*");
        }
        //
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        ////Khác
//        Intent chooser = new Intent(Intent.ACTION_GET_CONTENT);
//        //Uri uri = Uri.parse(Environment.getDownloadCacheDirectory().getPath().toString());
//        chooser.addCategory(Intent.CATEGORY_OPENABLE);
//        chooser.setDataAndType(uri, "*/*");
//        // startActivity(chooser);
//        try {
//            context.startActivity(chooser,SELECT_FILE);
//        }
//        catch (android.content.ActivityNotFoundException ex)
//        {
//            Toast.makeText(this, "Please install a File Manager.",
//                    Toast.LENGTH_SHORT).show();
//        }
        //////

    }
}
