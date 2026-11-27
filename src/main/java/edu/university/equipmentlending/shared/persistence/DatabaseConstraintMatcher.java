package edu.university.equipmentlending.shared.persistence;

/** Matches structured Hibernate constraint metadata without parsing database messages. */
public final class DatabaseConstraintMatcher {
    private DatabaseConstraintMatcher() {
    }

    public static boolean matches(Throwable failure, String sqlState, String constraintName) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && sqlState.equals(violation.getSQLState())
                    && constraintName.equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
