class Api::PostsControllerTest < ActionController::TestCase
  def setup
    $redis = MockRedis.new
  end

  test "users subscribed to a post's thread should get an email when a new post is made" do
    t = discussion_threads(:created_by_full_hacker_schooler)
    full_hacker_schooler = users(:full_hacker_schooler)

    login(:dave)

    post :create, format: :json, thread_id: t.id, post: {body: "A new post"}

    assert_equal 1, ActionMailer::Base.deliveries.size
    mail = ActionMailer::Base.deliveries.first

    assert_equal [full_hacker_schooler.email], mail.to
    assert_operator mail.text_part.body.to_s, :=~, /subscribed/
  end

  test "users subscribed and then unsubscribed from a post's thread shouldn't get an email when a new post is made" do
    t = discussion_threads(:created_by_full_hacker_schooler)
    full_hacker_schooler = users(:full_hacker_schooler)

    Subscription.where(subscribable: t, user: full_hacker_schooler).first.update(subscribed: false)

    login(:dave)

    post :create, format: :json, thread_id: t.id, post: {body: "A new post"}

    assert_equal [], ActionMailer::Base.deliveries
  end
end