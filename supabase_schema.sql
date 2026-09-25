-- ============================================================
-- Kapwa DVO — Supabase PostgreSQL Schema
-- Run this in the Supabase SQL Editor to set up all tables.
-- ============================================================

-- 1. profiles (extends auth.users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'owner', 'admin')),
    name TEXT,
    email TEXT,
    status TEXT DEFAULT 'active',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Auto-create profile on signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, email, role)
    VALUES (NEW.id, NEW.email, 'user')
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();


-- 2. listings
CREATE TABLE IF NOT EXISTS public.listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL DEFAULT 'Business',
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    address TEXT,
    hours TEXT,
    contact TEXT,
    photo_urls TEXT[] DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW()
);


-- 3. saved_locations
CREATE TABLE IF NOT EXISTS public.saved_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES public.listings(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id, listing_id)
);


-- 4. bookings
CREATE TABLE IF NOT EXISTS public.bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES public.listings(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    date TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'declined')),
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);


-- 5. owner_applications
CREATE TABLE IF NOT EXISTS public.owner_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    submitted_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id)
);


-- 6. reviews (one review & rating per user per listing)
CREATE TABLE IF NOT EXISTS public.reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES public.listings(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (listing_id, user_id)
);

-- 7. review_comments (unlimited discussion comments/replies per user)
CREATE TABLE IF NOT EXISTS public.review_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES public.listings(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reviews_listing ON public.reviews(listing_id);
CREATE INDEX IF NOT EXISTS idx_review_comments_listing ON public.review_comments(listing_id);


-- ============================================================
-- Row Level Security (RLS)
-- ============================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.saved_locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bookings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.owner_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.review_comments ENABLE ROW LEVEL SECURITY;

-- profiles
CREATE POLICY "Public read profiles" ON public.profiles FOR SELECT USING (true);
CREATE POLICY "Users update own profile" ON public.profiles FOR UPDATE USING (auth.uid() = id);
CREATE POLICY "Service role insert profiles" ON public.profiles FOR INSERT WITH CHECK (true);

-- listings
CREATE POLICY "Anyone can read approved listings" ON public.listings FOR SELECT
    USING (status = 'approved' OR auth.uid() = owner_id OR EXISTS (
        SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'
    ));
CREATE POLICY "Owners and admins can insert listings" ON public.listings FOR INSERT
    WITH CHECK (auth.uid() IS NOT NULL);
CREATE POLICY "Owners can update own listings" ON public.listings FOR UPDATE
    USING (auth.uid() = owner_id OR EXISTS (
        SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'
    ));
CREATE POLICY "Admins can delete listings" ON public.listings FOR DELETE
    USING (auth.uid() = owner_id OR EXISTS (
        SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'
    ));

-- saved_locations
CREATE POLICY "Users manage own saved" ON public.saved_locations
    USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- bookings
CREATE POLICY "Users read own bookings" ON public.bookings FOR SELECT
    USING (auth.uid() = user_id OR EXISTS (
        SELECT 1 FROM public.listings WHERE id = listing_id AND owner_id = auth.uid()
    ) OR EXISTS (
        SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'
    ));
CREATE POLICY "Users insert own bookings" ON public.bookings FOR INSERT
    WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Owners update booking status" ON public.bookings FOR UPDATE
    USING (EXISTS (
        SELECT 1 FROM public.listings WHERE id = listing_id AND owner_id = auth.uid()
    ) OR EXISTS (
        SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'
    ));

-- owner_applications
CREATE POLICY "Users read own application" ON public.owner_applications FOR SELECT
    USING (auth.uid() = user_id OR EXISTS (
        SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'
    ));
CREATE POLICY "Users insert own application" ON public.owner_applications FOR INSERT
    WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Admins update applications" ON public.owner_applications FOR UPDATE
    USING (EXISTS (
        SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'
    ));

-- reviews
CREATE POLICY "Anyone can read reviews" ON public.reviews FOR SELECT USING (true);
CREATE POLICY "Users insert own review" ON public.reviews FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users update own review" ON public.reviews FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users delete own review" ON public.reviews FOR DELETE USING (auth.uid() = user_id);

-- review_comments
CREATE POLICY "Anyone can read review comments" ON public.review_comments FOR SELECT USING (true);
CREATE POLICY "Users insert own review comments" ON public.review_comments FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users update own review comments" ON public.review_comments FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users delete own review comments" ON public.review_comments FOR DELETE USING (auth.uid() = user_id);


-- ============================================================
-- Seed: Create first admin user
-- After signing up via the app, run this (replace with real UUID):
-- UPDATE public.profiles SET role = 'admin' WHERE email = 'admin@kapwadvo.com';
-- ============================================================

-- ============================================================
-- Storage: listing_photos
-- ============================================================
INSERT INTO storage.buckets (id, name, public) VALUES ('listing_photos', 'listing_photos', true) ON CONFLICT DO NOTHING;

CREATE POLICY "Anyone can view listing_photos" ON storage.objects FOR SELECT USING (bucket_id = 'listing_photos');
CREATE POLICY "Authenticated users can upload listing_photos" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'listing_photos' AND auth.role() = 'authenticated');
CREATE POLICY "Owners can update listing_photos" ON storage.objects FOR UPDATE USING (bucket_id = 'listing_photos' AND auth.role() = 'authenticated');
CREATE POLICY "Owners can delete listing_photos" ON storage.objects FOR DELETE USING (bucket_id = 'listing_photos' AND auth.role() = 'authenticated');
