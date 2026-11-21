
package com.banking.system.service;

import com.banking.system.model.Customer;
import com.banking.system.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Customer registerCustomer(Customer customer) {

        if (customer.getFullName() == null ||
                customer.getFullName().isBlank()) {
            throw new IllegalArgumentException(
                    "Customer name is required"
            );
        }

        customer.setFullName(customer.getFullName().trim());

        return customerRepository.save(customer);
    }

    public Optional<Customer> getCustomerById(Long customerId) {

        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid customer ID"
            );
        }

        return customerRepository.findById(customerId);
    }
}
