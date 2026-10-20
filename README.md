# SENG300 Assignment 1 - GoCycle

```text
Gavin Kyle - 30196910 
```
## Run it

You need a JDK 24+ and Maven.

- **Windows:** double-click `start-gocycle.cmd`, or run `run-rider.cmd`.

## Rider menu

1. Sign up or switch rider (casual, member, student)
2. Find stations and vehicles
3. Start a trip (zone payment rules and the 20% battery rule apply)
4. Ride: let simulated time pass (time also passes on its own, 15 network seconds per real second)
5. Return my vehicle (full stations redirect you to the nearest free point; the trip ends only once the lock is confirmed)
6. Report a problem with a vehicle
7. My trips and receipts
8. Language, read-aloud and receipt settings
9. Exit

Seeded data: Eau Claire starts full (to try the redirect), ES-301 and EB-205 are below 20% battery, Kensington doesn't accept credit cards. Zone payments and branding are set in `config/zones.properties`.

## Notes

-   Repairs: the maintenance crew fixes broken vehicles after 45 simulated minutes.
-   Rebalancing: every 30 minutes, one ready vehicle is moved automatically from a full station to an empty one.
-   Rider terminal: Runs a simulated clock where each real second is 15 seconds of trip time, so batteries, costs and repairs keep moving on their own.
-   Zone Payment: Credit cards refused in Kensington
-   Charging: Electric vehicles recharge overtime