# application
frontend and backend development


- POST /api/signup: Registering a new user
- POST /api/org/{ordId}/login: Logging in
- GET /api/org/{orgId}/user: Retrieving a user’s profile (restricted to the user themselves)
- PATCH /api/org/{orgId}/user/:userId: Updating a user’s profile (restricted to the user themselves)
- GET /api/org/{orgId}/user/all: Retrieving all users (available to all users)
- PATCH /api/org/{orgId}/user/change-role/:userId: Updating a user’s role (restricted to admins)
- DELETE /api/org/{orgId}/user/:userId: Deleting a user (restricted to admins)

Design Article
- https://medium.com/@bhargavkanjarla01/how-to-combine-a-java-spring-boot-back-end-with-a-reactjs-front-end-app-ed8d8ca65285
- https://medium.com/@himanshu675/you-dont-need-spring-data-jpa-seriously-here-s-the-proof-ec768b638b68
