package com.qvety.reference;

import com.qvety.auth.CurrentUser;
import com.qvety.practice.PracticeRepository;
import org.springframework.stereotype.Service;

/**
 * The price list. Every rule lives in {@link ReferenceService} except one: the currency comes from the
 * practice row, never from the request, so a service can never be priced in a currency the clinic does
 * not bill in. Part 14 sums invoice lines and has no repair for a mixed-currency invoice.
 */
@Service
public class ClinicServiceService extends ReferenceService<ClinicService, ServiceRequest, ServiceDto> {

    private final ClinicServiceMapper mapper;
    private final PracticeRepository practices;
    private final CurrentUser currentUser;

    public ClinicServiceService(ClinicServiceRepository repository, ClinicServiceMapper mapper,
                                PracticeRepository practices, CurrentUser currentUser) {
        super(repository);
        this.mapper = mapper;
        this.practices = practices;
        this.currentUser = currentUser;
    }

    @Override
    protected ClinicService newEntity() {
        var service = new ClinicService();
        service.setCurrency(practiceCurrency());
        return service;
    }

    @Override
    protected void apply(ServiceRequest request, ClinicService service) {
        mapper.apply(request, service);
    }

    @Override
    protected ServiceDto toDto(ClinicService service) {
        return mapper.toDto(service);
    }

    @Override
    protected String name(ServiceRequest request) {
        return request.name();
    }

    /** RLS scopes the read to the caller's own practice row. */
    private String practiceCurrency() {
        return practices.findById(currentUser.practiceId()).orElseThrow().getCurrency();
    }
}
