package org.smartregister.chw.anc.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vijay.jsonwizard.customviews.CheckBox;

import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.anc.contract.BaseAncHomeVisitContract;
import org.smartregister.chw.anc.contract.BaseHomeVisitImmunizationFragmentContract;
import org.smartregister.chw.anc.domain.VaccineDisplay;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseHomeVisitImmunizationFragmentModel;
import org.smartregister.chw.anc.presenter.BaseHomeVisitImmunizationFragmentPresenter;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.anc.util.JsonFormUtils;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.opensrp_chw_anc.R;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.VaccineWrapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class BaseHomeVisitImmunizationFragment extends BaseHomeVisitFragment implements View.OnClickListener, BaseHomeVisitImmunizationFragmentContract.View {

    protected BaseAncHomeVisitContract.VisitView visitView;
    protected String baseEntityID;
    protected Map<String, List<VisitDetail>> details;
    protected BaseHomeVisitImmunizationFragmentContract.Presenter presenter;
    private List<VaccineView> vaccineViews = new ArrayList<>();
    private Map<String, VaccineDisplay> vaccineDisplays = new LinkedHashMap<>();
    private LayoutInflater inflater;
    private LinearLayout multipleVaccineDatePickerView, singleVaccineAddView, vaccinationNameLayout;
    private TextView textViewAddDate;
    private CheckBox checkBoxNoVaccinesDone;
    private DatePicker singleDatePicker;
    private Button saveButton;
    private SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMATS.NATIVE_FORMS, Locale.getDefault());

    public static BaseHomeVisitImmunizationFragment getInstance(final BaseAncHomeVisitContract.VisitView view, String baseEntityID, Map<String, List<VisitDetail>> details, List<VaccineDisplay> vaccineDisplays) {
        BaseHomeVisitImmunizationFragment fragment = new BaseHomeVisitImmunizationFragment();
        fragment.visitView = view;
        fragment.baseEntityID = baseEntityID;
        fragment.details = details;
        for (VaccineDisplay vaccineDisplay : vaccineDisplays) {
            fragment.vaccineDisplays.put(vaccineDisplay.getVaccineWrapper().getName(), vaccineDisplay);
        }

        if (details != null && details.size() > 0) {
            fragment.jsonObject = NCUtils.getVisitJSONFromVisitDetails(baseEntityID, details, vaccineDisplays);
            JsonFormUtils.populateForm(fragment.jsonObject, details);
        }

        return fragment;
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home_visit_immunization, container, false);
        this.inflater = inflater;

        multipleVaccineDatePickerView = view.findViewById(R.id.multiple_vaccine_date_pickerview);
        singleVaccineAddView = view.findViewById(R.id.single_vaccine_add_layout);
        vaccinationNameLayout = view.findViewById(R.id.vaccination_name_layout);

        saveButton = view.findViewById(R.id.save_btn);
        saveButton.setOnClickListener(this);

        view.findViewById(R.id.close).setOnClickListener(this);

        textViewAddDate = view.findViewById(R.id.add_date_separately);
        textViewAddDate.setOnClickListener(this);

        singleDatePicker = view.findViewById(R.id.earlier_date_picker);

        if (vaccineDisplays.size() > 0)
            initializeDatePicker(singleDatePicker, vaccineDisplays);

        checkBoxNoVaccinesDone = view.findViewById(R.id.select);
        checkBoxNoVaccinesDone.setOnClickListener(this);
        checkBoxNoVaccinesDone.setChecked(false);

        addVaccineViews();

        checkBoxNoVaccinesDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setSingleEntryMode(true);
                for (VaccineView vaccineView : vaccineViews) {
                    if (vaccineView.getCheckBox().isChecked())
                        vaccineView.getCheckBox().setChecked(false);
                }
            }

            redrawView();
        });

        initializePresenter();

        return view;
    }

    @Override
    public Map<String, VaccineDisplay> getVaccineDisplays() {
        return vaccineDisplays;
    }

    @Override
    public void setVaccineDisplays(Map<String, VaccineDisplay> vaccineDisplays) {
        this.vaccineDisplays = vaccineDisplays;

        // redraw all vaccine views
        if (vaccineDisplays.size() > 0 && singleDatePicker != null) {
            initializeDatePicker(singleDatePicker, vaccineDisplays);
            addVaccineViews();
        }

        // reset the json payload if the vaccine view was updated manually
        this.jsonObject = null;
    }

    @Override
    public void initializePresenter() {
        presenter = new BaseHomeVisitImmunizationFragmentPresenter(this, new BaseHomeVisitImmunizationFragmentModel());
    }

    private void addVaccineViews() {
        // get the views and bind the click listener
        vaccineViews.clear();
        for (Map.Entry<String, VaccineDisplay> entry : vaccineDisplays.entrySet()) {
            VaccineWrapper vaccineWrapper = entry.getValue().getVaccineWrapper();

            View vaccinationName = inflater.inflate(R.layout.custom_vaccine_name_check, null);
            TextView vaccineView = vaccinationName.findViewById(R.id.vaccine);
            CheckBox checkBox = vaccinationName.findViewById(R.id.select);
            VaccineRepo.Vaccine vaccine = vaccineWrapper.getVaccine();
            final VaccineView view = new VaccineView(vaccineWrapper.getName(), null, checkBox);
            vaccineView.setText((vaccineWrapper.getVaccine() != null) ? vaccine.display() : vaccineWrapper.getName());

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    checkBoxNoVaccinesDone.setChecked(false);
                } else {
                    // check if there are any active vaccine
                    boolean enableNoVaccines = true;
                    for (VaccineView vaccineView1 : vaccineViews) {
                        if (vaccineView1.getCheckBox().isChecked())
                            enableNoVaccines = false;
                    }

                    if (enableNoVaccines && !checkBoxNoVaccinesDone.isChecked())
                        checkBoxNoVaccinesDone.setChecked(true);
                }
                redrawView();
            });
            vaccinationNameLayout.addView(vaccinationName);
            vaccineViews.add(view);
        }
    }

    private void initializeDatePicker(@NotNull DatePicker datePicker, @NotNull VaccineDisplay vaccineDisplay) {
        Date startDate = vaccineDisplay.getStartDate();
        Date endDate = (vaccineDisplay.getEndDate() != null && vaccineDisplay.getEndDate().getTime() < new Date().getTime()) ?
                vaccineDisplay.getEndDate() : new Date();

        if (startDate.getTime() > endDate.getTime()) {
            datePicker.setMinDate(endDate.getTime());
            datePicker.setMaxDate(endDate.getTime());
        } else {
            datePicker.setMinDate(startDate.getTime());
            datePicker.setMaxDate(endDate.getTime());
        }
    }

    private void initializeDatePicker(@NotNull DatePicker datePicker, @NotNull Map<String, VaccineDisplay> vaccineDisplays) {
        //compute the start date and the end date
        Date startDate = null;
        Date endDate = new Date();
        for (Map.Entry<String, VaccineDisplay> entry : vaccineDisplays.entrySet()) {
            VaccineDisplay display = entry.getValue();

            // get the largest start date
            if (startDate == null || display.getStartDate().getTime() < startDate.getTime())
                startDate = display.getStartDate();

            // get the lowest end date
            if (display.getEndDate() != null && display.getEndDate().getTime() < endDate.getTime())
                endDate = display.getEndDate();
        }

        if (startDate != null && startDate.getTime() > endDate.getTime()) {
            datePicker.setMinDate(endDate.getTime());
            datePicker.setMaxDate(endDate.getTime());
        } else {
            datePicker.setMinDate(startDate != null ? startDate.getTime() : endDate.getTime());
            datePicker.setMaxDate(endDate.getTime());
        }
    }

    private Date getDateFromDatePicker(DatePicker datePicker) {
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();
        int year = datePicker.getYear();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);

        return calendar.getTime();
    }

    private void setDateFromDatePicker(DatePicker datePicker, Date date) {
        datePicker.init(date.getYear(), date.getMonth(), date.getDay(), null);
    }

    /**
     * activated when each vaccine has a different date
     */
    private void onVariedResponsesMode() {
        // global state
        setSingleEntryMode(false);

        // create a number of date piker views with the injected heading
        singleVaccineAddView.removeAllViews();
        generateDatePickerViews();
        redrawView();
    }

    private void generateDatePickerViews() {
        int x = 0;
        while (vaccineViews.size() > x) {
            VaccineView vaccineView = vaccineViews.get(x);

            View layout = inflater.inflate(R.layout.custom_single_vaccine_view, null);
            TextView question = layout.findViewById(R.id.vaccines_given_when_title_question);
            DatePicker datePicker = layout.findViewById(R.id.earlier_date_picker);
            question.setText(getString(R.string.when_vaccine, vaccineView.vaccineName));

            VaccineDisplay vaccineDisplay = vaccineDisplays.get(vaccineView.getVaccineName());
            if (vaccineDisplay != null)
                initializeDatePicker(datePicker, vaccineDisplay);
            vaccineView.setDatePickerView(datePicker);

            singleVaccineAddView.addView(layout);
            x++;
        }
    }

    /**
     * notifies the host view of all the selected values
     * by sending a json object with the details
     */
    private void onSave() {
        // notify the view (write to json file then dismiss)

        Date vaccineDate = getDateFromDatePicker(singleDatePicker);
        HashMap<VaccineWrapper, String> vaccineDateMap = new HashMap<>();

        boolean multiModeActive = multipleVaccineDatePickerView.getVisibility() == View.GONE;

        for (VaccineView vaccineView : vaccineViews) {
            VaccineDisplay display = vaccineDisplays.get(vaccineView.getVaccineName());
            VaccineWrapper wrapper = display.getVaccineWrapper();
            if (wrapper != null) {
                if (!checkBoxNoVaccinesDone.isChecked() && vaccineView.getCheckBox().isChecked()) {
                    if (vaccineView.getDatePickerView() != null && multiModeActive) {
                        Date dateGiven = getDateFromDatePicker(vaccineView.getDatePickerView());
                        vaccineDateMap.put(wrapper, dateFormat.format(dateGiven));
                        display.setDateGiven(dateGiven);
                        display.setValid(true);
                    } else if (vaccineDate != null) {
                        vaccineDateMap.put(wrapper, dateFormat.format(vaccineDate));
                        display.setDateGiven(vaccineDate);
                        display.setValid(true);
                    }
                } else {
                    vaccineDateMap.put(wrapper, Constants.HOME_VISIT.VACCINE_NOT_GIVEN);
                    display.setDateGiven(null);
                    display.setValid(false);
                }
            }
        }

        // create a json object and write values to it that have the vaccine dates
        jsonObject = NCUtils.getVisitJSONFromWrapper(baseEntityID, vaccineDateMap);

        // notify the view
        if (jsonObject != null) {
            visitView.onDialogOptionUpdated(jsonObject.toString());

            // save the view
            dismiss();
        }
    }

    /**
     * reset the view payload
     */
    public void resetViewPayload() {
        jsonObject = null;
        visitView.onDialogOptionUpdated("");
    }

    /**
     * executed to close the vaccine screen
     */
    private void onClose() {
        dismiss();
    }

    /**
     * Is called on every ui updating action
     */
    public void redrawView() {
        boolean noVaccine = true;
        for (VaccineView vaccineView : vaccineViews) {
            // enable or disable views
            if (vaccineView.getDatePickerView() != null) {
                ((View) vaccineView.getDatePickerView().getParent()).setVisibility(vaccineView.getCheckBox().isChecked() ? View.VISIBLE : View.GONE);
            }

            if (vaccineView.getCheckBox().isChecked())
                noVaccine = false;
        }

        if (noVaccine) {
            multipleVaccineDatePickerView.setAlpha(0.3f);
        } else {
            multipleVaccineDatePickerView.setAlpha(1.0f);
            saveButton.setAlpha(1.0f);
        }
    }

    /**
     * toggle vaccine entry mode
     *
     * @param state
     */
    private void setSingleEntryMode(boolean state) {
        textViewAddDate.setVisibility(state ? View.VISIBLE : View.GONE);
        multipleVaccineDatePickerView.setVisibility(state ? View.VISIBLE : View.GONE);
        singleVaccineAddView.setVisibility(state ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        int viewID = v.getId();
        if (viewID == R.id.save_btn) {
            onSave();
        } else if (viewID == R.id.add_date_separately) {
            onVariedResponsesMode();
        } else if (viewID == R.id.close) {
            onClose();
        }
    }

    @Override
    public void updateNoVaccineCheck(boolean state) {
        checkBoxNoVaccinesDone.setChecked(state);
    }

    @Override
    public void updateSelectedVaccines(Map<String, String> selectedVaccines, boolean variedMode) {
        if (variedMode)
            generateDatePickerViews();

        Map<String, VaccineView> lookup = new HashMap<>();
        for (VaccineView vaccineView : vaccineViews) {
            lookup.put(NCUtils.removeSpaces(vaccineView.vaccineName), vaccineView);
        }

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMATS.NATIVE_FORMS, Locale.getDefault());
        for (Map.Entry<String, String> entry : selectedVaccines.entrySet()) {
            VaccineView vaccineView = lookup.get(entry.getKey());
            if (vaccineView != null) {
                if (entry.getValue().equalsIgnoreCase(Constants.HOME_VISIT.VACCINE_NOT_GIVEN)) {
                    vaccineView.getCheckBox().setChecked(false);
                } else {
                    vaccineView.getCheckBox().setChecked(true);
                    try {
                        DatePicker datePicker = vaccineView.getDatePickerView();
                        if (datePicker == null)
                            datePicker = singleDatePicker;

                        setDateFromDatePicker(datePicker, sdf.parse(entry.getValue()));
                    } catch (ParseException e) {
                        Timber.e(e);
                    }
                }
            }
        }

        setSingleEntryMode(!variedMode);
    }

    /**
     * holding container
     */
    private class VaccineView {
        private String vaccineName;
        private DatePicker datePickerView;
        private CheckBox checkBox;

        public VaccineView(String vaccineName, DatePicker datePickerView, CheckBox checkBox) {
            this.vaccineName = vaccineName;
            this.datePickerView = datePickerView;
            this.checkBox = checkBox;
        }

        public String getVaccineName() {
            return vaccineName;
        }

        public DatePicker getDatePickerView() {
            return datePickerView;
        }

        public void setDatePickerView(DatePicker datePickerView) {
            this.datePickerView = datePickerView;
        }

        public CheckBox getCheckBox() {
            return checkBox;
        }

        public void setCheckBox(CheckBox checkBox) {
            this.checkBox = checkBox;
        }
    }
}
