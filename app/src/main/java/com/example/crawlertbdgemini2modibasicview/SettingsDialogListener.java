package com.example.crawlertbdgemini2modibasicview;

public interface SettingsDialogListener {
    void onSettingsSaved(boolean isArea1Checked, boolean isOption1Checked, boolean isOption2Checked);

    void onSettingsSaved(String selectedIdName);
}
