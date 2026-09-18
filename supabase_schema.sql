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


-- ============================================================
-- Row Level Security (RLS)
-- ============================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.saved_locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bookings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.owner_applications ENABLE ROW LEVEL SECURITY;

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


-- ============================================================
-- Seed: Create first admin user
-- After signing up via the app, run this (replace with real UUID):
-- UPDATE public.profiles SET role = 'admin' WHERE email = 'admin@kapwadvo.com';
-- ============================================================
