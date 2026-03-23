--liquibase formatted sql
--changeset arinkmm:2
CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_link_id ON subscriptions(link_id);
CREATE INDEX idx_subscription_tags_user_id ON subscription_tags(user_id, link_id);
CREATE INDEX idx_links_last_checked ON links(last_checked);
