package com.medreport.ai.activities;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.medreport.ai.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class EditSectionDialog extends Dialog {

    public interface OnSaveListener {
        void onSave(String section, JsonObject updatedAnalysis);
    }

    private final String section;
    private final JsonObject aiAnalysis;
    private final OnSaveListener listener;
    private LinearLayout formContainer;
    private final List<EditText> fieldInputs = new ArrayList<>();
    private final List<String> fieldKeys = new ArrayList<>();

    private final List<EditText> arrayInputs = new ArrayList<>();
    private LinearLayout arrayContainer;

    private final List<MedRow> medRows = new ArrayList<>();
    private LinearLayout medsContainer;

    private final List<LabRow> labRows = new ArrayList<>();
    private LinearLayout labsContainer;

    static class MedRow {
        EditText name, dosage, frequency;
    }
    static class LabRow {
        EditText testName, value, unit, refRange, status;
    }

    public EditSectionDialog(@NonNull Context context, String section, JsonObject aiAnalysis, OnSaveListener listener) {
        super(context, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        this.section = section;
        this.aiAnalysis = aiAnalysis != null ? aiAnalysis.deepCopy() : new JsonObject();
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(getContext());
        scroll.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background));
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(40));

        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(20));

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText("Edit " + getSectionTitle());
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(tvTitle);

        MaterialButton btnCancel = new MaterialButton(getContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnCancel.setText("Cancel");
        btnCancel.setAllCaps(false);
        btnCancel.setCornerRadius(dp(10));
        btnCancel.setOnClickListener(v -> dismiss());
        header.addView(btnCancel);
        root.addView(header);

        formContainer = new LinearLayout(getContext());
        formContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(formContainer);

        buildForm();

        MaterialButton btnSave = new MaterialButton(getContext());
        btnSave.setText("Save Changes");
        btnSave.setAllCaps(false);
        btnSave.setCornerRadius(dp(12));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        slp.topMargin = dp(20);
        btnSave.setLayoutParams(slp);
        btnSave.setOnClickListener(v -> save());
        root.addView(btnSave);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void buildForm() {
        switch (section) {
            case "patient_info":   buildPatientInfoForm(); break;
            case "diagnoses":      buildArrayForm("diagnoses", "Diagnosis"); break;
            case "symptoms":       buildArrayForm("symptoms", "Symptom"); break;
            case "medications":    buildMedicationsForm(); break;
            case "vital_signs":    buildVitalSignsForm(); break;
            case "lab_results":    buildLabResultsForm(); break;
        }
    }

    private void buildPatientInfoForm() {
        JsonObject pi = getObj("patient_info");
        addField("Name", "name", safeStr(pi, "name"));
        addField("Age", "age", safeStr(pi, "age"));
        addField("Gender", "gender", safeStr(pi, "gender"));
        addField("Blood Group", "blood_group", safeStr(pi, "blood_group"));
        addField("Patient ID", "patient_id", safeStr(pi, "patient_id"));
        addField("Contact", "contact", safeStr(pi, "contact"));
        addField("Address", "address", safeStr(pi, "address"));
    }

    private void buildArrayForm(String key, String itemLabel) {
        arrayContainer = new LinearLayout(getContext());
        arrayContainer.setOrientation(LinearLayout.VERTICAL);
        formContainer.addView(arrayContainer);
        
        JsonArray arr = getArray(key);
        if (arr != null) {
            for (JsonElement e : arr) {
                try { addArrayRow(e.getAsString()); } catch (Exception ex) { addArrayRow(e.toString()); }
            }
        }
        
        MaterialButton btnAdd = new MaterialButton(getContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnAdd.setText("+ Add " + itemLabel);
        btnAdd.setAllCaps(false);
        btnAdd.setCornerRadius(dp(10));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
        alp.topMargin = dp(12);
        btnAdd.setLayoutParams(alp);
        btnAdd.setOnClickListener(v -> {
            addArrayRow("");
            arrayInputs.get(arrayInputs.size() - 1).requestFocus();
        });
        formContainer.addView(btnAdd);
    }

    private void addArrayRow(String value) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rl.bottomMargin = dp(8);
        row.setLayoutParams(rl);

        EditText et = createInput("Enter value...");
        et.setText(value);
        et.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(et);
        arrayInputs.add(et);

        MaterialButton btnDel = new MaterialButton(getContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnDel.setText("✕");
        btnDel.setMinimumWidth(dp(40));
        btnDel.setMinWidth(dp(40));
        btnDel.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(44), dp(44));
        dlp.leftMargin = dp(8);
        btnDel.setLayoutParams(dlp);
        btnDel.setTextColor(ContextCompat.getColor(getContext(), R.color.danger));
        btnDel.setStrokeColorResource(R.color.danger);
        btnDel.setCornerRadius(dp(10));
        btnDel.setOnClickListener(v -> {
            arrayInputs.remove(et);
            arrayContainer.removeView(row);
        });
        row.addView(btnDel);

        arrayContainer.addView(row);
    }

    private void buildMedicationsForm() {
        medsContainer = new LinearLayout(getContext());
        medsContainer.setOrientation(LinearLayout.VERTICAL);
        formContainer.addView(medsContainer);

        JsonArray meds = getArray("current_medications");
        if (meds != null) {
            for (JsonElement e : meds) {
                if (e.isJsonObject()) {
                    JsonObject m = e.getAsJsonObject();
                    addMedRow(safeStr(m, "name", safeStr(m, "medication", "")),
                              safeStr(m, "dosage", safeStr(m, "dose", "")),
                              safeStr(m, "frequency", ""));
                } else {
                    try { addMedRow(e.getAsString(), "", ""); } catch (Exception ex) {}
                }
            }
        }
        
        MaterialButton btnAdd = new MaterialButton(getContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnAdd.setText("+ Add Medication");
        btnAdd.setAllCaps(false);
        btnAdd.setCornerRadius(dp(10));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
        alp.topMargin = dp(12);
        btnAdd.setLayoutParams(alp);
        btnAdd.setOnClickListener(v -> addMedRow("", "", ""));
        formContainer.addView(btnAdd);
    }

    private void addMedRow(String name, String dosage, String freq) {
        MedRow mr = new MedRow();
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_badge);
        card.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0x08000000));
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(10);
        card.setLayoutParams(clp);

        mr.name = createInput("Medication name");
        mr.name.setText(name);
        card.addView(mr.name);
        addSpacer(card, 8);

        mr.dosage = createInput("Dosage (e.g., 10mg)");
        mr.dosage.setText(dosage);
        card.addView(mr.dosage);
        addSpacer(card, 8);

        mr.frequency = createInput("Frequency (e.g., Daily)");
        mr.frequency.setText(freq);
        card.addView(mr.frequency);

        MaterialButton btnDel = new MaterialButton(getContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnDel.setText("Remove");
        btnDel.setAllCaps(false);
        btnDel.setCornerRadius(dp(8));
        btnDel.setTextColor(ContextCompat.getColor(getContext(), R.color.danger));
        btnDel.setStrokeColorResource(R.color.danger);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
        dlp.topMargin = dp(8);
        btnDel.setLayoutParams(dlp);
        btnDel.setOnClickListener(v -> {
            medRows.remove(mr);
            medsContainer.removeView(card);
        });
        card.addView(btnDel);

        medRows.add(mr);
        medsContainer.addView(card);
    }

    private void buildVitalSignsForm() {
        JsonObject vs = getObj("vital_signs");
        String[] keys = {"blood_pressure", "heart_rate", "temperature", "respiratory_rate", "oxygen_saturation", "weight", "height", "bmi"};
        String[] labels = {"Blood Pressure", "Heart Rate", "Temperature", "Respiratory Rate", "O₂ Saturation", "Weight", "Height", "BMI"};
        for (int i = 0; i < keys.length; i++) {
            addField(labels[i], keys[i], safeStr(vs, keys[i]));
        }
    }

    private void buildLabResultsForm() {
        labsContainer = new LinearLayout(getContext());
        labsContainer.setOrientation(LinearLayout.VERTICAL);
        formContainer.addView(labsContainer);

        JsonArray labs = getArray("lab_results");
        if (labs != null) {
            for (JsonElement e : labs) {
                if (e.isJsonObject()) {
                    JsonObject l = e.getAsJsonObject();
                    addLabRow(safeStr(l, "test_name", safeStr(l, "name", "")),
                              safeStr(l, "value", ""),
                              safeStr(l, "unit", safeStr(l, "units", "")),
                              safeStr(l, "reference_range", safeStr(l, "normal_range", "")),
                              safeStr(l, "status", safeStr(l, "flag", "")));
                }
            }
        }
        
        MaterialButton btnAdd = new MaterialButton(getContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnAdd.setText("+ Add Lab Result");
        btnAdd.setAllCaps(false);
        btnAdd.setCornerRadius(dp(10));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
        alp.topMargin = dp(12);
        btnAdd.setLayoutParams(alp);
        btnAdd.setOnClickListener(v -> addLabRow("", "", "", "", ""));
        formContainer.addView(btnAdd);
    }

    private void addLabRow(String testName, String value, String unit, String refRange, String status) {
        LabRow lr = new LabRow();
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_badge);
        card.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0x08000000));
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(10);
        card.setLayoutParams(clp);

        lr.testName = createInput("Test name");
        lr.testName.setText(testName);
        card.addView(lr.testName);
        addSpacer(card, 6);

        LinearLayout valRow = new LinearLayout(getContext());
        valRow.setOrientation(LinearLayout.HORIZONTAL);
        lr.value = createInput("Value");
        lr.value.setText(value);
        lr.value.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        valRow.addView(lr.value);

        View sp = new View(getContext());
        sp.setLayoutParams(new LinearLayout.LayoutParams(dp(8), 1));
        valRow.addView(sp);

        lr.unit = createInput("Unit");
        lr.unit.setText(unit);
        lr.unit.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        valRow.addView(lr.unit);
        card.addView(valRow);
        addSpacer(card, 6);

        lr.refRange = createInput("Reference range");
        lr.refRange.setText(refRange);
        card.addView(lr.refRange);
        addSpacer(card, 6);

        lr.status = createInput("Status (Normal / Abnormal)");
        lr.status.setText(status);
        card.addView(lr.status);

        MaterialButton btnDel = new MaterialButton(getContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnDel.setText("Remove");
        btnDel.setAllCaps(false);
        btnDel.setCornerRadius(dp(8));
        btnDel.setTextColor(ContextCompat.getColor(getContext(), R.color.danger));
        btnDel.setStrokeColorResource(R.color.danger);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
        dlp.topMargin = dp(8);
        btnDel.setLayoutParams(dlp);
        btnDel.setOnClickListener(v -> {
            labRows.remove(lr);
            labsContainer.removeView(card);
        });
        card.addView(btnDel);

        labRows.add(lr);
        labsContainer.addView(card);
    }


    private void save() {
        JsonObject updated = aiAnalysis.deepCopy();

        switch (section) {
            case "patient_info": {
                JsonObject pi = new JsonObject();
                for (int i = 0; i < fieldKeys.size(); i++) {
                    String val = fieldInputs.get(i).getText().toString().trim();
                    if (!val.isEmpty()) {
                        if (fieldKeys.get(i).equals("age")) {
                            try { pi.addProperty(fieldKeys.get(i), Integer.parseInt(val)); } catch (Exception e) { pi.addProperty(fieldKeys.get(i), val); }
                        } else {
                            pi.addProperty(fieldKeys.get(i), val);
                        }
                    }
                }
                updated.add("patient_info", pi);
                break;
            }
            case "diagnoses": {
                JsonArray arr = new JsonArray();
                for (EditText et : arrayInputs) {
                    String val = et.getText().toString().trim();
                    if (!val.isEmpty()) arr.add(val);
                }
                updated.add("diagnoses", arr);
                break;
            }
            case "symptoms": {
                JsonArray arr = new JsonArray();
                for (EditText et : arrayInputs) {
                    String val = et.getText().toString().trim();
                    if (!val.isEmpty()) arr.add(val);
                }
                updated.add("symptoms", arr);
                break;
            }
            case "medications": {
                JsonArray arr = new JsonArray();
                for (MedRow mr : medRows) {
                    String n = mr.name.getText().toString().trim();
                    if (n.isEmpty()) continue;
                    JsonObject m = new JsonObject();
                    m.addProperty("name", n);
                    String d = mr.dosage.getText().toString().trim();
                    if (!d.isEmpty()) m.addProperty("dosage", d);
                    String f = mr.frequency.getText().toString().trim();
                    if (!f.isEmpty()) m.addProperty("frequency", f);
                    arr.add(m);
                }
                updated.add("current_medications", arr);
                break;
            }
            case "vital_signs": {
                JsonObject vs = new JsonObject();
                for (int i = 0; i < fieldKeys.size(); i++) {
                    String val = fieldInputs.get(i).getText().toString().trim();
                    if (!val.isEmpty()) vs.addProperty(fieldKeys.get(i), val);
                }
                updated.add("vital_signs", vs);
                break;
            }
            case "lab_results": {
                JsonArray arr = new JsonArray();
                for (LabRow lr : labRows) {
                    String tn = lr.testName.getText().toString().trim();
                    if (tn.isEmpty()) continue;
                    JsonObject l = new JsonObject();
                    l.addProperty("test_name", tn);
                    String v = lr.value.getText().toString().trim();
                    if (!v.isEmpty()) l.addProperty("value", v);
                    String u = lr.unit.getText().toString().trim();
                    if (!u.isEmpty()) l.addProperty("unit", u);
                    String r = lr.refRange.getText().toString().trim();
                    if (!r.isEmpty()) l.addProperty("reference_range", r);
                    String s = lr.status.getText().toString().trim();
                    if (!s.isEmpty()) l.addProperty("status", s);
                    arr.add(l);
                }
                updated.add("lab_results", arr);
                break;
            }
        }

        listener.onSave(section, updated);
        dismiss();
    }


    private void addField(String label, String key, String value) {
        TextView tv = new TextView(getContext());
        tv.setText(label);
        tv.setTextSize(13);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        tv.setPadding(0, 0, 0, dp(4));
        formContainer.addView(tv);

        EditText et = createInput("Enter " + label.toLowerCase() + "...");
        if (value != null && !value.equals("null")) et.setText(value);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        elp.bottomMargin = dp(14);
        et.setLayoutParams(elp);
        formContainer.addView(et);

        fieldKeys.add(key);
        fieldInputs.add(et);
    }

    private EditText createInput(String hint) {
        EditText et = new EditText(getContext());
        et.setHint(hint);
        et.setBackgroundResource(R.drawable.bg_input_field);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        et.setTextSize(14);
        et.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        et.setHintTextColor(ContextCompat.getColor(getContext(), R.color.text_tertiary));
        return et;
    }

    private void addSpacer(LinearLayout parent, int dpHeight) {
        View v = new View(getContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(dpHeight)));
        parent.addView(v);
    }

    private String getSectionTitle() {
        switch (section) {
            case "patient_info": return "Patient Information";
            case "diagnoses":    return "Diagnoses";
            case "symptoms":     return "Symptoms";
            case "medications":  return "Medications";
            case "vital_signs":  return "Vital Signs";
            case "lab_results":  return "Lab Results";
            default: return section;
        }
    }

    private JsonObject getObj(String key) {
        try {
            if (aiAnalysis.has(key) && !aiAnalysis.get(key).isJsonNull() && aiAnalysis.get(key).isJsonObject())
                return aiAnalysis.getAsJsonObject(key);
        } catch (Exception e) {}
        return new JsonObject();
    }

    private JsonArray getArray(String key) {
        try {
            if (aiAnalysis.has(key) && !aiAnalysis.get(key).isJsonNull() && aiAnalysis.get(key).isJsonArray())
                return aiAnalysis.getAsJsonArray(key);
        } catch (Exception e) {}
        return new JsonArray();
    }

    private String safeStr(JsonObject obj, String key) {
        if (obj == null) return "";
        try {
            if (obj.has(key) && !obj.get(key).isJsonNull()) return obj.get(key).getAsString();
        } catch (Exception e) {}
        return "";
    }

    private String safeStr(JsonObject obj, String key, String fallback) {
        if (obj == null) return fallback;
        try {
            if (obj.has(key) && !obj.get(key).isJsonNull()) return obj.get(key).getAsString();
        } catch (Exception e) {}
        return fallback;
    }

    private int dp(int v) {
        return Math.round(v * getContext().getResources().getDisplayMetrics().density);
    }
}
