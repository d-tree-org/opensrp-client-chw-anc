package org.smartregister.chw.anc.actionhelper;

import org.json.JSONObject;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.anc.util.JsonFormUtils;
import org.smartregister.chw.opensrp_chw_anc.R;

import java.text.MessageFormat;

import timber.log.Timber;

public class DangerSignsHelper extends HomeVisitActionHelper {
    private String signs_present;
    private String counseling;

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            signs_present = JsonFormUtils.getCheckBoxValue(jsonObject, "danger_signs_present");
            counseling = JsonFormUtils.getValue(jsonObject, "danger_signs_counseling");
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public String evaluateSubTitle() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(MessageFormat.format("{0}: {1}", context.getString(R.string.anc_home_visit_danger_signs), signs_present));
        stringBuilder.append("\n");
        stringBuilder.append(MessageFormat.format("{0} {1}",
                context.getString(R.string.danger_signs_counselling),
                (counseling.equalsIgnoreCase("Yes") ? context.getString(R.string.done).toLowerCase() : context.getString(R.string.not_done).toLowerCase())
        ));

        if (counseling.equalsIgnoreCase("Yes") || counseling.equalsIgnoreCase("No"))
            return stringBuilder.toString();

        return null;
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (counseling.equalsIgnoreCase("Yes")) {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        } else if (counseling.equalsIgnoreCase("No")) {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.PENDING;
        }
    }

}