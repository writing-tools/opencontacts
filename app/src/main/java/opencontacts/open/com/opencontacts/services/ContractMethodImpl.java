package opencontacts.open.com.opencontacts.services;

import android.content.Context;

import com.github.underscore.Function;

import java.util.List;

import open.com.opencontactsdatasourcecontract.ContractMethod;

public class ContractMethodImpl {
    private final Function<List<String>, Function<Context, String>> implementation;
    public ContractMethod contractMethod;

    public ContractMethodImpl(ContractMethod contractMethod, Function<List<String>, Function<Context, String>> implementation) {
        this.contractMethod = contractMethod;
        this.implementation = implementation;
    }
    public String call(List<String> args, Context context) {
        return implementation.apply(args).apply(context);
    }
}
