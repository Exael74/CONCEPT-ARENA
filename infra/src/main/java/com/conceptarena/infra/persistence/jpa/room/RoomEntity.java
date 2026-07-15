package com.conceptarena.infra.persistence.jpa.room;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
public class RoomEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    private String inviteCode;

    @Column(nullable = false)
    private String conceptBankId;

    @Column(nullable = false)
    private int maxParticipants;

    @Column(nullable = false)
    private String status;

    @OneToMany(mappedBy = "roomId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParticipantEntity> participants = new ArrayList<>();

    public RoomEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public String getConceptBankId() { return conceptBankId; }
    public void setConceptBankId(String conceptBankId) { this.conceptBankId = conceptBankId; }
    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<ParticipantEntity> getParticipants() { return participants; }
    public void setParticipants(List<ParticipantEntity> participants) { this.participants = participants; }
}
