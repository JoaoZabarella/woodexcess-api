package com.z.c.woodexcess_api.validator;

import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;

public record ValidatedMessageData (
        User sender,
        User recipient,
        MaterialListing listing
){
}
