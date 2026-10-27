package sisso.logicsim.classroom.persistence;

import java.sql.*;

final class LearningSchema {
    private LearningSchema() { }
    static void create(Connection c) throws SQLException {
        for(String sql:new String[]{
            "CREATE TABLE learning_activities(id TEXT PRIMARY KEY NOT NULL,classroom_id TEXT NOT NULL REFERENCES classrooms(id),revision INTEGER NOT NULL DEFAULT 0,archived INTEGER NOT NULL DEFAULT 0 CHECK(archived IN(0,1)),has_draft INTEGER NOT NULL DEFAULT 1 CHECK(has_draft IN(0,1)),draft_title TEXT NOT NULL DEFAULT '',draft_description TEXT NOT NULL DEFAULT '',draft_deadline INTEGER,draft_value INTEGER,current_version INTEGER)",
            "CREATE INDEX learning_activity_class ON learning_activities(classroom_id,archived)",
            "CREATE TABLE learning_versions(activity_id TEXT NOT NULL REFERENCES learning_activities(id),number INTEGER NOT NULL,title TEXT NOT NULL,description TEXT NOT NULL,deadline INTEGER NOT NULL,value INTEGER NOT NULL CHECK(value>0),published_at INTEGER NOT NULL,PRIMARY KEY(activity_id,number))",
            "CREATE TABLE learning_submissions(id TEXT PRIMARY KEY NOT NULL,activity_id TEXT NOT NULL,publication INTEGER NOT NULL,student_id TEXT NOT NULL,membership_id TEXT NOT NULL REFERENCES classroom_memberships(id),revision INTEGER NOT NULL CHECK(revision>0),received_at INTEGER NOT NULL,late INTEGER NOT NULL CHECK(late IN(0,1)),content_id TEXT NOT NULL UNIQUE,checksum TEXT NOT NULL CHECK(length(checksum)=64),bytes INTEGER NOT NULL CHECK(bytes>0),width INTEGER NOT NULL,height INTEGER NOT NULL,depth INTEGER NOT NULL,UNIQUE(activity_id,student_id,revision),FOREIGN KEY(activity_id,publication) REFERENCES learning_versions(activity_id,number))",
            "CREATE TABLE learning_grades(submission_id TEXT NOT NULL REFERENCES learning_submissions(id),revision INTEGER NOT NULL,score INTEGER NOT NULL CHECK(score>=0),feedback TEXT NOT NULL,graded_at INTEGER NOT NULL,teacher_id TEXT NOT NULL,PRIMARY KEY(submission_id,revision))",
            "CREATE TABLE learning_reviews(id TEXT PRIMARY KEY NOT NULL,teacher_id TEXT NOT NULL UNIQUE,submission_id TEXT NOT NULL REFERENCES learning_submissions(id),world_id TEXT NOT NULL,x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,width INTEGER NOT NULL,height INTEGER NOT NULL,depth INTEGER NOT NULL)",
            "CREATE INDEX learning_review_world ON learning_reviews(world_id)"
        }) try(var s=c.createStatement()) { s.execute(sql); }
    }
    static void upgradeSix(Connection c) throws SQLException {
        try(var s=c.createStatement()) {
            s.execute("ALTER TABLE learning_activities ADD COLUMN draft_rules TEXT NOT NULL DEFAULT ''");
            s.execute("ALTER TABLE learning_versions ADD COLUMN rules TEXT NOT NULL DEFAULT ''");
        }
    }
    static void verify(Connection c) throws SQLException {
        for(String sql:new String[]{"SELECT id,classroom_id,revision,archived,has_draft,draft_title,draft_description,draft_deadline,draft_value,current_version FROM learning_activities LIMIT 0",
                "SELECT draft_rules FROM learning_activities LIMIT 0",
                "SELECT activity_id,number,title,description,deadline,value,published_at,rules FROM learning_versions LIMIT 0",
                "SELECT id,activity_id,publication,student_id,membership_id,revision,received_at,late,content_id,checksum,bytes,width,height,depth FROM learning_submissions LIMIT 0",
                "SELECT submission_id,revision,score,feedback,graded_at,teacher_id FROM learning_grades LIMIT 0",
                "SELECT id,teacher_id,submission_id,world_id,x,y,z,width,height,depth FROM learning_reviews LIMIT 0"})
            try(var s=c.createStatement()) { s.execute(sql); }
    }
}
