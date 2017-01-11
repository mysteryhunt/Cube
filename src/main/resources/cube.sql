CREATE TABLE run (
       startTimestamp TIMESTAMP DEFAULT NULL
);
INSERT INTO run (startTimestamp) VALUES (NULL);

CREATE TABLE teams (
       teamId VARCHAR(64),
       email VARCHAR(320),
       headquarters VARCHAR(64),
       primaryPhone VARCHAR(30),
       secondaryPhone VARCHAR(30),
       PRIMARY KEY(teamId)
);

CREATE TABLE team_properties (
       teamId VARCHAR(64),
       propertyKey VARCHAR(64),
       propertyValue TEXT,
       PRIMARY KEY(teamId, propertyKey),
       FOREIGN KEY(teamId) REFERENCES teams(teamId)
);

CREATE TABLE users (
       username VARCHAR(64),
       password VARCHAR(128),
       password_salt VARCHAR(24),
       teamId VARCHAR(64),
       PRIMARY KEY(username),
       FOREIGN KEY(teamId) REFERENCES teams(teamId)
);

CREATE TABLE roles (
       role_name VARCHAR(64),
       PRIMARY KEY(role_name)
);

CREATE TABLE roles_permissions (
       role_name VARCHAR(64),
       permission VARCHAR(64),
       FOREIGN KEY(role_name) REFERENCES roles(role_name)
);

CREATE TABLE user_roles (
       username VARCHAR(64),
       role_name VARCHAR(64),
       FOREIGN KEY(username) REFERENCES users(username),
       FOREIGN KEY(role_name) REFERENCES roles(role_name)
);

CREATE TABLE users_permissions (
       username VARCHAR(64),
       permission VARCHAR(64),
       FOREIGN KEY(username) REFERENCES users(username)
);

CREATE TABLE puzzles (
       puzzleId VARCHAR(64),
       PRIMARY KEY(puzzleId)
);

CREATE TABLE puzzle_properties (
       puzzleId VARCHAR(64),
       propertyKey VARCHAR(64),
       propertyValue TEXT,
       PRIMARY KEY(puzzleId, propertyKey),
       FOREIGN KEY(puzzleId) REFERENCES puzzles(puzzleId)
);

CREATE TABLE puzzle_indexable_properties (
       puzzleId VARCHAR(64),
       propertyKey VARCHAR(64),
       propertyValue VARCHAR(96),
       PRIMARY KEY(puzzleId, propertyKey),
       FOREIGN KEY(puzzleId) REFERENCES puzzles(puzzleId)
);

CREATE INDEX puzzle_indexable_properties_index
       ON puzzle_indexable_properties (propertyKey, propertyValue);

CREATE TABLE submissions (
       submissionId ${auto_increment_type},
       teamId VARCHAR(64),
       puzzleId VARCHAR(64),
       submission TEXT,
       timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       status VARCHAR(10) DEFAULT 'SUBMITTED',
       callerUsername VARCHAR(64),
       canonicalAnswer TEXT,
       PRIMARY KEY(submissionId),
       FOREIGN KEY(teamId) REFERENCES teams(teamId),
       FOREIGN KEY(puzzleId) REFERENCES puzzles(puzzleId),
       FOREIGN KEY(callerUsername) REFERENCES users(username)
);

CREATE TABLE visibilities (
       teamId VARCHAR(64),
       puzzleId VARCHAR(64),
       status VARCHAR(10) DEFAULT '${default_visibility_status}',
       PRIMARY KEY(teamId, puzzleId),
       FOREIGN KEY(teamId) REFERENCES teams(teamId),
       FOREIGN KEY(puzzleId) REFERENCES puzzles(puzzleId)
);

CREATE TABLE visibility_history (
       visibilityHistoryId ${auto_increment_type},
       teamId VARCHAR(64),
       puzzleId VARCHAR(64),
       status VARCHAR(10) DEFAULT '${default_visibility_status}',
       timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       PRIMARY KEY(visibilityHistoryId),
       FOREIGN KEY(teamId) REFERENCES teams(teamId),
       FOREIGN KEY(puzzleId) REFERENCES puzzles(puzzleId)
);

CREATE TABLE hint_requests (
       hintRequestId ${auto_increment_type},
       teamId VARCHAR(64),
       puzzleId VARCHAR(64),
       status VARCHAR(10) DEFAULT 'REQUESTED',
       timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       request TEXT,
       response TEXT,
       callerUsername VARCHAR(64),
       PRIMARY KEY(hintRequestId),
       FOREIGN KEY(teamId) REFERENCES teams(teamId),
       FOREIGN KEY(puzzleId) REFERENCES puzzles(puzzleId),
       FOREIGN KEY(callerUsername) REFERENCES users(username)
);

CREATE TABLE interaction_requests (
       interactionRequestId ${auto_increment_type},
       teamId VARCHAR(64),
       puzzleId VARCHAR(64),
       status VARCHAR(10) DEFAULT 'REQUESTED',
       timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       request TEXT,
       response TEXT,
       callerUsername VARCHAR(64),
       PRIMARY KEY(interactionRequestId),
       FOREIGN KEY(teamId) REFERENCES teams(teamId),
       FOREIGN KEY(puzzleId) REFERENCES puzzles(puzzleId),
       FOREIGN KEY(callerUsername) REFERENCES users(username)
);

CREATE VIEW submissions_with_display_ids AS
SELECT
  submissions.submissionId as submissionId,
  submissions.teamId as teamId,
  submissions.puzzleId as canonicalPuzzleId,
  puzzle_indexable_properties.propertyValue as displayPuzzleId,
  submissions.submission as submission,
  submissions.timestamp as timestamp,
  submissions.status as status,
  submissions.callerUsername as callerUsername,
  submissions.canonicalAnswer as canonicalAnswer
FROM submissions
JOIN puzzle_indexable_properties
ON submissions.puzzleId = puzzle_indexable_properties.puzzleId
WHERE
  puzzle_indexable_properties.propertyKey = 'DisplayIdProperty'
;

CREATE VIEW visibilities_with_display_ids AS
SELECT
  visibilities.teamId as teamId,
  visibilities.puzzleId as canonicalPuzzleId,
  puzzle_indexable_properties.propertyValue as displayPuzzleId,
  visibilities.status as status
FROM visibilities
JOIN puzzle_indexable_properties
ON visibilities.puzzleId = puzzle_indexable_properties.puzzleId
WHERE
  puzzle_indexable_properties.propertyKey = 'DisplayIdProperty'
;

CREATE VIEW open_times AS
SELECT
  visibility_history.teamId AS teamId,
  visibility_history.puzzleId AS canonicalPuzzleId,
  puzzle_indexable_properties.propertyValue AS displayPuzzleId,
  ${timestamp_to_number_prefix}${current_timestamp} - visibility_history.timestamp${timestamp_to_number_suffix} / 60 AS unlockedMinutes
FROM visibility_history
INNER JOIN visibilities
ON
  visibility_history.teamId = visibilities.teamId AND
  visibility_history.puzzleId = visibilities.puzzleId
JOIN puzzle_indexable_properties
ON visibility_history.puzzleId = puzzle_indexable_properties.puzzleId
WHERE
  visibility_history.status = 'UNLOCKED' AND
  visibilities.status = 'UNLOCKED' AND
  puzzle_indexable_properties.propertyKey = 'DisplayIdProperty'
;

CREATE VIEW solve_times AS
SELECT
  unlock_history.teamId AS teamId,
  unlock_history.puzzleId AS canonicalPuzzleId,
  puzzle_indexable_properties.propertyValue AS displayPuzzleId,
  ${timestamp_to_number_prefix}solve_history.timestamp - unlock_history.timestamp${timestamp_to_number_suffix} / 60 AS solveMinutes
FROM visibility_history AS unlock_history
INNER JOIN visibility_history AS solve_history
ON
  unlock_history.teamId = solve_history.teamId AND
  unlock_history.puzzleId = solve_history.puzzleId
JOIN puzzle_indexable_properties
ON unlock_history.puzzleId = puzzle_indexable_properties.puzzleId
WHERE
  unlock_history.status = 'UNLOCKED' AND
  solve_history.status = 'SOLVED' AND
  puzzle_indexable_properties.propertyKey = 'DisplayIdProperty'
;
