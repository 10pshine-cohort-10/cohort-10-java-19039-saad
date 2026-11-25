package com.cms.repository;

import com.cms.entity.Contact;
import com.cms.entity.ContactEmail;
import com.cms.entity.ContactPhoneNumber;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ContactSpecifications {

    private ContactSpecifications() {
    }

    public static Specification<Contact> forUserAndSearch(Long userId, String query, String company, String city) {
        return (root, criteriaQuery, cb) -> {
            criteriaQuery.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            // Mandatory tenant scope: contact must belong to this user
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            // Optional filter by company
            if (company != null && !company.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("company")), "%" + company.trim().toLowerCase() + "%"));
            }

            // Optional filter by city
            if (city != null && !city.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("city")), "%" + city.trim().toLowerCase() + "%"));
            }

            // Global search query
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";

                Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), pattern);
                Predicate lastNameMatch = cb.like(cb.lower(root.get("lastName")), pattern);
                Predicate companyMatch = cb.like(cb.lower(root.get("company")), pattern);
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);

                // Full name: CONCAT(first_name, ' ', last_name)
                Predicate fullNameMatch = cb.like(
                        cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))),
                        pattern
                );

                // Email match
                Join<Contact, ContactEmail> emailJoin = root.join("emails", JoinType.LEFT);
                Predicate emailMatch = cb.like(cb.lower(emailJoin.get("email")), pattern);

                // Phone match
                Join<Contact, ContactPhoneNumber> phoneJoin = root.join("phoneNumbers", JoinType.LEFT);
                Predicate phoneMatch = cb.like(cb.lower(phoneJoin.get("phoneNumber")), pattern);

                predicates.add(cb.or(firstNameMatch, lastNameMatch, companyMatch, titleMatch, fullNameMatch, emailMatch, phoneMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
