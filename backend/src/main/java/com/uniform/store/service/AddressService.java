package com.uniform.store.service;

import com.uniform.store.dto.request.CreateAddressRequest;
import com.uniform.store.dto.request.UpdateAddressRequest;
import com.uniform.store.dto.response.AddressDto;

import java.util.List;

public interface AddressService {

    List<AddressDto> listAddresses(String email);

    AddressDto createAddress(String email, CreateAddressRequest req);

    AddressDto updateAddress(String email, Long addressId, UpdateAddressRequest req);

    void deleteAddress(String email, Long addressId);

    AddressDto setDefault(String email, Long addressId);
}
