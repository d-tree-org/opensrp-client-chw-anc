package provider;


import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Years;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.smartregister.chw.anc.fragment.BaseAncRegisterFragment;
import org.smartregister.chw.anc.util.DBConstants;
import org.smartregister.chw.pnc.R;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.RecyclerViewProvider;
import org.smartregister.util.Utils;
import org.smartregister.view.contract.SmartRegisterClient;
import org.smartregister.view.contract.SmartRegisterClients;
import org.smartregister.view.dialog.FilterOption;
import org.smartregister.view.dialog.ServiceModeOption;
import org.smartregister.view.dialog.SortOption;
import org.smartregister.view.viewholder.OnClickFormLauncher;

import java.text.MessageFormat;
import java.util.Set;

import static org.smartregister.util.Utils.getName;

public class PncRegisterProvider implements RecyclerViewProvider<PncRegisterProvider.RegisterViewHolder> {

    private final LayoutInflater inflater;
    private Set<org.smartregister.configurableviews.model.View> visibleColumns;

    private android.view.View.OnClickListener onClickListener;
    private android.view.View.OnClickListener paginationClickListener;

    private Context context;
    private CommonRepository commonRepository;

    public PncRegisterProvider(Context context, CommonRepository commonRepository, Set visibleColumns, android.view.View.OnClickListener onClickListener, android.view.View.OnClickListener paginationClickListener) {
//        TODO add onClickListener and commonRepository to constructor

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.visibleColumns = visibleColumns;

        this.onClickListener = onClickListener;
        this.paginationClickListener = paginationClickListener;

        this.context = context;
        this.commonRepository = commonRepository;
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, PncRegisterProvider.RegisterViewHolder viewHolder) {
        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;
        if (visibleColumns.isEmpty()) {
            populatePatientColumn(pc, client, viewHolder);
            populateLastColumn(pc, viewHolder);

            return;
        }
    }

    private void populatePatientColumn(CommonPersonObjectClient pc, SmartRegisterClient client, final RegisterViewHolder viewHolder) {

        viewHolder.villageTown.setText(Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.VILLAGE_TOWN, true));

        String fname = getName(
                Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true),
                Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.MIDDLE_NAME, true)
        );

        String patientName = getName(fname, Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.LAST_NAME, true));

        String dobString = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.DOB, false);
        if (StringUtils.isNotBlank(dobString)) {
            int age = Years.yearsBetween(new DateTime(dobString), new DateTime()).getYears();
            String patientNameAge = MessageFormat.format("{0}, {1}",
                    patientName,
                    age
            );
            viewHolder.patientNameAndAge.setText(patientNameAge);
        } else {
            viewHolder.patientNameAndAge.setText(patientName);
        }

        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd-MM-yyyy");
        String dayPnc = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.DELIVERY_DATE, true);
        if (StringUtils.isNotBlank(dayPnc)) {
            int period = Days.daysBetween(new DateTime(formatter.parseDateTime(dayPnc)), new DateTime()).getDays();
            String pncDay = MessageFormat.format("{0} {1}",
                    context.getString(R.string.pnc_day),
                    period
            );
            viewHolder.pncDay.setText(pncDay);
        }

        // add patient listener
        viewHolder.patientColumn.setOnClickListener(onClickListener);
        viewHolder.patientColumn.setTag(client);
        viewHolder.patientColumn.setTag(org.smartregister.chw.opensrp_chw_anc.R.id.VIEW_ID, BaseAncRegisterFragment.CLICK_VIEW_NORMAL);

        // add due listener
        viewHolder.dueButton.setOnClickListener(onClickListener);
        viewHolder.dueButton.setTag(client);
        viewHolder.dueButton.setTag(org.smartregister.chw.opensrp_chw_anc.R.id.VIEW_ID, BaseAncRegisterFragment.CLICK_VIEW_DOSAGE_STATUS);

        viewHolder.registerColumns.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                viewHolder.patientColumn.performClick();
            }
        });

        viewHolder.dueWrapper.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                viewHolder.dueButton.performClick();
            }
        });
    }

    private void populateLastColumn(CommonPersonObjectClient pc, RegisterViewHolder viewHolder) {
        if (commonRepository != null) {
            CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(pc.entityId());
            if (commonPersonObject != null) {
                viewHolder.dueButton.setVisibility(android.view.View.VISIBLE);
                viewHolder.dueButton.setText(context.getString(org.smartregister.chw.opensrp_chw_anc.R.string.anc_home_visit));
                viewHolder.dueButton.setAllCaps(true);
            } else {
                viewHolder.dueButton.setVisibility(android.view.View.GONE);
            }
        }
    }

    @Override
    public void getFooterView(RecyclerView.ViewHolder viewHolder, int currentPageCount, int totalPageCount, boolean hasNext, boolean hasPrevious) {
        FooterViewHolder footerViewHolder = (FooterViewHolder) viewHolder;
        footerViewHolder.pageInfoView.setText(
                MessageFormat.format(context.getString(org.smartregister.R.string.str_page_info), currentPageCount,
                        totalPageCount));

        footerViewHolder.nextPageView.setVisibility(hasNext ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        footerViewHolder.previousPageView.setVisibility(hasPrevious ? android.view.View.VISIBLE : android.view.View.INVISIBLE);

        footerViewHolder.nextPageView.setOnClickListener(paginationClickListener);
        footerViewHolder.previousPageView.setOnClickListener(paginationClickListener);
    }

    @Override
    public SmartRegisterClients updateClients(FilterOption villageFilter, ServiceModeOption serviceModeOption, FilterOption searchFilter, SortOption sortOption) {
        return null;
    }

    @Override
    public void onServiceModeSelected(ServiceModeOption serviceModeOption) {
//        implement

    }

    @Override
    public OnClickFormLauncher newFormLauncher(String s, String s1, String s2) {
        return null;
    }

    @Override
    public LayoutInflater inflater() {
        return inflater;
    }

    @Override
    public PncRegisterProvider.RegisterViewHolder createViewHolder(ViewGroup parent) {
        android.view.View view = inflater.inflate(R.layout.pnc_register_list_row, parent, false);
        return new PncRegisterProvider.RegisterViewHolder(view);
    }

    @Override
    public RecyclerView.ViewHolder createFooterHolder(ViewGroup parent) {
        android.view.View view = inflater.inflate(org.smartregister.chw.opensrp_chw_anc.R.layout.smart_register_pagination, parent, false);
        return new PncRegisterProvider.FooterViewHolder(view);
    }

    @Override
    public boolean isFooterViewHolder(RecyclerView.ViewHolder viewHolder) {
        return viewHolder instanceof FooterViewHolder;
    }

    // implement place holder view
    public class RegisterViewHolder extends RecyclerView.ViewHolder {
        public TextView patientNameAndAge;
        public TextView villageTown;
        public TextView textViewHasReferral;
        public TextView pncDay;
        public Button dueButton;
        public android.view.View patientColumn;

        public android.view.View registerColumns;
        public android.view.View dueWrapper;

        public RegisterViewHolder(android.view.View itemView) {
            super(itemView);

            patientNameAndAge = itemView.findViewById(R.id.patient_name_and_age);
            pncDay = itemView.findViewById(R.id.pnc_period);

            villageTown = itemView.findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.village_town);
            textViewHasReferral = itemView.findViewById(R.id.has_referral);
            dueButton = itemView.findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.due_button);

            patientColumn = itemView.findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.patient_column);

            registerColumns = itemView.findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.register_columns);
            dueWrapper = itemView.findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.due_button_wrapper);
        }
    }

    // implement footer view
    public class FooterViewHolder extends RecyclerView.ViewHolder {
        public TextView pageInfoView;
        public Button nextPageView;
        public Button previousPageView;

        public FooterViewHolder(android.view.View view) {
            super(view);

            nextPageView = view.findViewById(org.smartregister.R.id.btn_next_page);
            previousPageView = view.findViewById(org.smartregister.R.id.btn_previous_page);
            pageInfoView = view.findViewById(org.smartregister.R.id.txt_page_info);
        }
    }
}
