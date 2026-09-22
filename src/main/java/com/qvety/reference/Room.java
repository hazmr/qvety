package com.qvety.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A consultation room, theatre, or kennel. Deactivated when it closes; past appointments keep it. */
@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
public class Room extends ReferenceEntity {
}
