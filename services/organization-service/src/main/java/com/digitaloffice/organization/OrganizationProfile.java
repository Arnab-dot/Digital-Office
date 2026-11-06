package com.digitaloffice.organization;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "organization_profiles")
public class OrganizationProfile {

    @Id
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "jurisdiction")
    private String jurisdiction;

    @Column(name = "incorporation_date")
    private LocalDate incorporationDate;

    @Column(name = "industry")
    private String industry;

    @Column(name = "company_size")
    private String companySize;

    @Column(name = "stage")
    private String stage;

    @Column(name = "website")
    private String website;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "country")
    private String country;

    @Column(name = "city")
    private String city;

    @Column(name = "address")
    private String address;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "signatory_name")
    private String signatoryName;

    @Column(name = "signatory_title")
    private String signatoryTitle;

    @Column(name = "signatory_email")
    private String signatoryEmail;

    @Column(name = "signatory_phone")
    private String signatoryPhone;

    @Column(name = "incorporation_doc_name")
    private String incorporationDocName;

    @Column(name = "tax_doc_name")
    private String taxDocName;

    @Column(name = "pitch_deck_doc_name")
    private String pitchDeckDocName;

    @Column(name = "allowed_email_domain")
    private String allowedEmailDomain;

    @Column(name = "enforce_company_domain")
    private Boolean enforceCompanyDomain = false;

    @Column(name = "verification_status", nullable = false)
    private String verificationStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrganizationProfile() {}

    public OrganizationProfile(UUID organizationId, String entityType, String legalName) {
        this.organizationId = organizationId;
        this.entityType = entityType;
        this.legalName = legalName;
        this.verificationStatus = "PENDING";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getOrganizationId() { return organizationId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }
    public LocalDate getIncorporationDate() { return incorporationDate; }
    public void setIncorporationDate(LocalDate incorporationDate) { this.incorporationDate = incorporationDate; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public String getCompanySize() { return companySize; }
    public void setCompanySize(String companySize) { this.companySize = companySize; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getSignatoryName() { return signatoryName; }
    public void setSignatoryName(String signatoryName) { this.signatoryName = signatoryName; }
    public String getSignatoryTitle() { return signatoryTitle; }
    public void setSignatoryTitle(String signatoryTitle) { this.signatoryTitle = signatoryTitle; }
    public String getSignatoryEmail() { return signatoryEmail; }
    public void setSignatoryEmail(String signatoryEmail) { this.signatoryEmail = signatoryEmail; }
    public String getSignatoryPhone() { return signatoryPhone; }
    public void setSignatoryPhone(String signatoryPhone) { this.signatoryPhone = signatoryPhone; }
    public String getIncorporationDocName() { return incorporationDocName; }
    public void setIncorporationDocName(String incorporationDocName) { this.incorporationDocName = incorporationDocName; }
    public String getTaxDocName() { return taxDocName; }
    public void setTaxDocName(String taxDocName) { this.taxDocName = taxDocName; }
    public String getPitchDeckDocName() { return pitchDeckDocName; }
    public void setPitchDeckDocName(String pitchDeckDocName) { this.pitchDeckDocName = pitchDeckDocName; }
    public String getAllowedEmailDomain() { return allowedEmailDomain; }
    public void setAllowedEmailDomain(String allowedEmailDomain) { this.allowedEmailDomain = allowedEmailDomain; }
    public Boolean getEnforceCompanyDomain() { return enforceCompanyDomain != null && enforceCompanyDomain; }
    public void setEnforceCompanyDomain(Boolean enforceCompanyDomain) { this.enforceCompanyDomain = enforceCompanyDomain; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

