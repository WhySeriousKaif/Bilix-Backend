# Molla POS Backend Interview Preparation

This folder is the revision notebook for the Spring Boot backend of the Molla POS system.

The explanations here use two references:

1. The current backend source under `backend/src`.
2. The course transcript supplied in this Codex chat (`Pasted text.txt`, approximately 95,000 words).

The transcript explains the order in which the instructor built the project. The source code is the authority for how the current project actually works. When they differ, notes should explicitly distinguish:

- what the instructor taught;
- what the current project implements;
- what should be said in an interview;
- any correction or production-level improvement.

## Notes

1. [JJWT dependencies and the role of Jackson](notes/01-jjwt-dependencies-and-jackson.md)
2. [SecurityContextHolder explained using Express.js](notes/02-security-context-holder.md)
3. [JWT authentication flow: MERN vs Spring Boot](notes/03-jwt-authentication-flow-mern-vs-spring-boot.md)
4. [Database choice and trade-offs for the POS system](notes/04-database-choice-and-tradeoffs.md)
5. [Spring Security configuration basics](notes/05-spring-security-configuration-basics.md)

## Topics to add as questions arise

- Spring Initializr and Maven project setup
- Spring Boot starters and dependency management
- Application properties and environment variables
- Dependency injection and Spring beans
- Controller, service, repository, DTO, and mapper layers
- JPA entities and relationships
- Authentication and authorization
- Spring Security filter chain
- JWT generation, validation, and request processing
- Validation and global exception handling
- Caching
- Transactions and concurrency
- Testing with JUnit, Mockito, and Spring Boot Test
- POS workflows: stores, branches, inventory, orders, refunds, and shifts

## Note format

Each future note should contain:

- the question;
- a beginner-friendly explanation;
- how it appears in this project;
- an interview-ready answer;
- likely follow-up questions;
- corrections or production considerations.
