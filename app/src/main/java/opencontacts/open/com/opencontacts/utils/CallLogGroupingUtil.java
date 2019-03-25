package opencontacts.open.com.opencontacts.utils;

import android.support.annotation.NonNull;

import com.github.underscore.U;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import opencontacts.open.com.opencontacts.domain.GroupedCallLogEntry;
import opencontacts.open.com.opencontacts.orm.CallLogEntry;

import static java.util.Calendar.HOUR;

public class CallLogGroupingUtil {

    public static final int OFFSET_TIME_IN_HOURS_FOR_GROUPING = 1;

    public static List<GroupedCallLogEntry> group(List<CallLogEntry> callLogEntries){
        ArrayList<GroupedCallLogEntry> groupedCallLogEntries = new ArrayList<>();
        if(U.isEmpty(callLogEntries)) {
            return groupedCallLogEntries;
        }


        groupedCallLogEntries.add(createGroupedCallLogEntry(callLogEntries.get(0)));

        return U.reduce(U.drop(callLogEntries, 1), (accumulator, callLogEntry)->{
            GroupedCallLogEntry lastGroup = U.last(accumulator);
            if(canBeGrouped(lastGroup, callLogEntry))
                lastGroup.callLogEntries = U.concat(lastGroup.callLogEntries, Collections.singletonList(callLogEntry));
            else
                accumulator.add(createGroupedCallLogEntry(callLogEntry));
            return accumulator;
        }, groupedCallLogEntries);
    }

    private static boolean canBeGrouped(GroupedCallLogEntry groupedCallLogEntry, CallLogEntry callLogEntry){
        if(callLogEntry.contactId != -1 && groupedCallLogEntry.latestCallLogEntry.contactId != callLogEntry.contactId)
            return false;
        if(callLogEntry.contactId == -1 && !groupedCallLogEntry.latestCallLogEntry.getPhoneNumber().equals(callLogEntry.getPhoneNumber()))
            return false;
        Calendar hourOffsetCalendarInstance = Common.getCalendarOffset(- OFFSET_TIME_IN_HOURS_FOR_GROUPING, HOUR, new Date(Long.parseLong(groupedCallLogEntry.latestCallLogEntry.getDate())));
        return (Common.getCalendarInstanceAt(Long.parseLong(callLogEntry.getDate())).after(hourOffsetCalendarInstance));
    }

    @NonNull
    private static GroupedCallLogEntry createGroupedCallLogEntry(CallLogEntry callLogEntry) {
        return new GroupedCallLogEntry(Collections.singletonList(callLogEntry), callLogEntry);
    }

}
