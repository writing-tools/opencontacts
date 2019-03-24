package opencontacts.open.com.opencontacts.domain;

import java.util.List;

import opencontacts.open.com.opencontacts.orm.CallLogEntry;

public class GroupedCallLogEntry {
    public List<CallLogEntry> callLogEntries;
    public CallLogEntry latestCallLogEntry;

    public GroupedCallLogEntry(List<CallLogEntry> callLogEntries, CallLogEntry latestCallLogEntry) {
        this.callLogEntries = callLogEntries;
        this.latestCallLogEntry = latestCallLogEntry;
    }
}
