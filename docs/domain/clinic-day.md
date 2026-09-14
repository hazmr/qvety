# A day in an Egyptian veterinary clinic

Draft written with Claude on 2026-09-14. Rewrite in your own words after one day sitting at a real front desk. Every feature must trace back to a sentence here.

## Opening the day (front desk)

The front desk opens the clinic around 10:00. On paper today there is a list: who is booked, with which vet, in which room. The desk checks the list against the phone: two clients called last night to cancel or move. The vet on duty asks "who is first". The desk also has a second list, written by hand, of pets whose vaccine is due this week; someone is supposed to call them between patients. The paper list is the **whiteboard**; the second list is the **recall** list.

## A client arrives with a pet

A **client** walks in holding a cat. The desk asks for the name and phone. Names are said as a chain (Ahmed Mohamed Ali), and the same person may spell it differently each time. If the client has been here before, the desk finds the **patient** card by the owner's phone or name; if not, a new card is opened for the client and one for the animal (name, **species**, breed, sex, age). Walk-ins are normal; more than half of a day is unbooked. The client waits; the desk marks them as waiting on the board. A family often brings two animals at once: two patients, one client, one bill.

## The vet examines

The **veterinarian** takes the patient into the exam **room**. The **visit** starts. A **technician** weighs the animal and takes the temperature (**vitals**). The vet looks at the card first: what was given last time, any **allergy**. The vet writes the **clinical note**: what the owner reports, what the vet finds, what it probably is, what to do (**SOAP**). If a vaccine is given, the vet or technician records the vaccine name, lot, date, and when the next dose is due (**vaccination record**). Once the vet signs the note it must never change; a later correction is an **addendum**.

## Prescription

Most clinics do not stock every drug. The vet writes a **prescription** (روشتة) on paper with the clinic header, the vet's name and **license number**, the drug, dose, and how long. The client takes it to a pharmacy. Sometimes the clinic dispenses from its own shelf; then the quantity leaves the clinic and, for a **controlled substance**, is written in a book with the running balance.

## Payment

Back at the desk the client pays. The desk adds up the consultation, the vaccine, anything dispensed (**invoice**). Prices are the clinic's **services** list, but a **discount** is common and negotiated on the spot. Payment is cash, InstaPay, or Vodafone Cash; for transfers the desk writes the transfer number (**external reference**). Surgery is often paid in parts: a deposit now, the rest on discharge. Some visits are free (a recheck, a friend); the desk must still know the visit happened and was not simply forgotten (**unbilled visit**). At the end of the day the owner wants one number per method: how much cash, how much by transfer.

## The reminder for next time

The next vaccine is due in three or four weeks. The clinic that calls the owner the week before gets the visit; the one that does not loses it to the shop next door. Today this is the hand-written list and a staff member's own phone. Tomorrow it is the **recall** list with a WhatsApp button that opens the message on the same phone. Every call is written down: reached, no answer, wrong number, booked. Non-vaccine follow-ups (deworming, a post-surgery check) go on the same list as **care reminders**.

## What is not on day one

- The old paper cards stay in the drawer. Each client is entered on their next visit; nothing is imported.
- When power or internet drops, the desk goes back to paper for an hour and enters it afterwards. There is no offline mode.
- Boarding, grooming, and inventory are not part of the pilot. They go on the backlog when a clinic asks.

## What the software must hold

- Clients and patients, findable by Arabic name variants and by any spelling of the phone.
- Today's appointments and walk-ins, and who is in the building now.
- The visit: vitals, a signed note, vaccinations, prescriptions, attachments. Never silently edited.
- A printable prescription and a printable vaccination card, Arabic and English.
- Invoices with a discount line, payments by method, the unbilled list, the daily cash total.
- Recalls computed from vaccinations, and care reminders, with every contact attempt recorded.
- Who changed what (audit), and the clinic's data exportable at any time.
- All of it in Arabic, right-to-left, with English available.
