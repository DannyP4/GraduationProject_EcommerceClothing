package com.uniform.store.service.impl;

import com.uniform.store.dto.request.CreateAddressRequest;
import com.uniform.store.dto.request.UpdateAddressRequest;
import com.uniform.store.dto.response.AddressDto;
import com.uniform.store.entity.Address;
import com.uniform.store.entity.User;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.AddressRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressServiceImpl implements AddressService {

    private static final String DEFAULT_COUNTRY = "VN";

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    @Override
    public List<AddressDto> listAddresses(String email) {
        User user = loadUser(email);
        return addressRepository.findByUserIdOrderByIsDefaultDescIdAsc(user.getId())
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public AddressDto createAddress(String email, CreateAddressRequest req) {
        User user = loadUser(email);

        // First address is always default
        boolean firstAddress = addressRepository.countByUserId(user.getId()) == 0;
        boolean shouldBeDefault = firstAddress || Boolean.TRUE.equals(req.getIsDefault());

        if (shouldBeDefault && !firstAddress) {
            addressRepository.clearDefaultsForUser(user.getId());
        }

        Address address = Address.builder()
                .user(user)
                .label(req.getLabel())
                .recipient(req.getRecipient().trim())
                .phone(req.getPhone().trim())
                .line1(req.getLine1().trim())
                .ward(req.getWard())
                .district(req.getDistrict().trim())
                .city(req.getCity().trim())
                .country(req.getCountry() != null ? req.getCountry() : DEFAULT_COUNTRY)
                .postalCode(req.getPostalCode())
                .region(req.getRegion())
                .isDefault(shouldBeDefault)
                .build();

        return toDto(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressDto updateAddress(String email, Long addressId, UpdateAddressRequest req) {
        User user = loadUser(email);
        Address address = loadOwnedAddress(user.getId(), addressId);

        // PATCH semantics: only set non-null fields.
        if (req.getLabel() != null) address.setLabel(req.getLabel());
        if (req.getRecipient() != null) address.setRecipient(req.getRecipient().trim());
        if (req.getPhone() != null) address.setPhone(req.getPhone().trim());
        if (req.getLine1() != null) address.setLine1(req.getLine1().trim());
        if (req.getWard() != null) address.setWard(req.getWard());
        if (req.getDistrict() != null) address.setDistrict(req.getDistrict().trim());
        if (req.getCity() != null) address.setCity(req.getCity().trim());
        if (req.getCountry() != null) address.setCountry(req.getCountry());
        if (req.getPostalCode() != null) address.setPostalCode(req.getPostalCode());
        if (req.getRegion() != null) address.setRegion(req.getRegion());

        return toDto(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(String email, Long addressId) {
        User user = loadUser(email);
        Address address = loadOwnedAddress(user.getId(), addressId);
        boolean wasDefault = address.isDefault();

        addressRepository.delete(address);

        // Auto-promote: never leave the user with addresses but no default.
        if (wasDefault) {
            addressRepository.flush();
            Optional<Address> next = addressRepository.findFirstByUserIdOrderByIdAsc(user.getId());
            next.ifPresent(a -> {
                a.setDefault(true);
                addressRepository.save(a);
            });
        }
    }

    @Override
    @Transactional
    public AddressDto setDefault(String email, Long addressId) {
        User user = loadUser(email);
        Address address = loadOwnedAddress(user.getId(), addressId);

        if (!address.isDefault()) {
            addressRepository.clearDefaultsForUser(user.getId());
            address.setDefault(true);
            addressRepository.save(address);
        }
        return toDto(address);
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Address loadOwnedAddress(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
    }

    private AddressDto toDto(Address a) {
        return AddressDto.builder()
                .id(a.getId())
                .label(a.getLabel())
                .recipient(a.getRecipient())
                .phone(a.getPhone())
                .line1(a.getLine1())
                .ward(a.getWard())
                .district(a.getDistrict())
                .city(a.getCity())
                .country(a.getCountry())
                .postalCode(a.getPostalCode())
                .region(a.getRegion())
                .isDefault(a.isDefault())
                .build();
    }
}
