-- ============================================================================
-- ReelLog — Personal Daily Reel Work Tracker
-- Supabase PostgreSQL Schema Migration & Security Rules
-- ============================================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. TABLE: clients
CREATE TABLE IF NOT EXISTS public.clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    client_name TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_user_client_name UNIQUE (user_id, client_name)
);

-- Index on clients
CREATE INDEX IF NOT EXISTS idx_clients_user_id ON public.clients(user_id);
CREATE INDEX IF NOT EXISTS idx_clients_is_active ON public.clients(user_id, is_active);

-- 3. TABLE: daily_reports
CREATE TABLE IF NOT EXISTS public.daily_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    work_date DATE NOT NULL,
    status TEXT NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SUBMITTED')),
    total_reels INTEGER NOT NULL DEFAULT 0,
    submitted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_user_work_date UNIQUE (user_id, work_date)
);

-- Index on daily_reports
CREATE INDEX IF NOT EXISTS idx_daily_reports_user_date ON public.daily_reports(user_id, work_date DESC);
CREATE INDEX IF NOT EXISTS idx_daily_reports_status ON public.daily_reports(user_id, status);

-- 4. TABLE: daily_report_entries
CREATE TABLE IF NOT EXISTS public.daily_report_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES public.daily_reports(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES public.clients(id) ON DELETE RESTRICT,
    reel_codes TEXT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_report_client UNIQUE (report_id, client_id)
);

-- Index on daily_report_entries
CREATE INDEX IF NOT EXISTS idx_report_entries_report ON public.daily_report_entries(report_id);
CREATE INDEX IF NOT EXISTS idx_report_entries_client ON public.daily_report_entries(client_id);

-- ============================================================================
-- 5. IMMUTABILITY TRIGGER: PREVENT MODIFICATION OF SUBMITTED REPORTS
-- ============================================================================

CREATE OR REPLACE FUNCTION public.check_submitted_report_immutability()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    -- Prevent modification of submitted daily_reports
    IF TG_TABLE_NAME = 'daily_reports' THEN
        IF TG_OP = 'DELETE' THEN
            IF OLD.status = 'SUBMITTED' THEN
                RAISE EXCEPTION 'Submitted daily reports are permanently immutable and cannot be deleted.';
            END IF;
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            -- Prevent changing status back from SUBMITTED to DRAFT
            IF OLD.status = 'SUBMITTED' AND NEW.status <> 'SUBMITTED' THEN
                RAISE EXCEPTION 'A submitted daily report cannot be reverted back to draft.';
            END IF;
            -- If already submitted, prevent editing any fields except maybe updated_at
            IF OLD.status = 'SUBMITTED' AND (
                OLD.work_date <> NEW.work_date OR
                OLD.total_reels <> NEW.total_reels OR
                OLD.user_id <> NEW.user_id
            ) THEN
                RAISE EXCEPTION 'Submitted daily reports are permanently immutable and cannot be altered.';
            END IF;
            NEW.updated_at = timezone('utc'::text, now());
            RETURN NEW;
        END IF;
    END IF;

    -- Prevent modifications to daily_report_entries belonging to submitted reports
    IF TG_TABLE_NAME = 'daily_report_entries' THEN
        IF TG_OP = 'INSERT' THEN
            IF EXISTS (SELECT 1 FROM public.daily_reports WHERE id = NEW.report_id AND status = 'SUBMITTED') THEN
                RAISE EXCEPTION 'Cannot add reel entries to an already submitted daily report.';
            END IF;
            NEW.updated_at = timezone('utc'::text, now());
            RETURN NEW;
        ELSIF TG_OP = 'UPDATE' THEN
            IF EXISTS (SELECT 1 FROM public.daily_reports WHERE id = OLD.report_id AND status = 'SUBMITTED') THEN
                RAISE EXCEPTION 'Cannot update reel entries in an already submitted daily report.';
            END IF;
            NEW.updated_at = timezone('utc'::text, now());
            RETURN NEW;
        ELSIF TG_OP = 'DELETE' THEN
            IF EXISTS (SELECT 1 FROM public.daily_reports WHERE id = OLD.report_id AND status = 'SUBMITTED') THEN
                RAISE EXCEPTION 'Cannot delete reel entries from an already submitted daily report.';
            END IF;
            RETURN OLD;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

-- Drop existing triggers if re-running
DROP TRIGGER IF EXISTS trg_protect_daily_reports ON public.daily_reports;
CREATE TRIGGER trg_protect_daily_reports
    BEFORE UPDATE OR DELETE ON public.daily_reports
    FOR EACH ROW
    EXECUTE FUNCTION public.check_submitted_report_immutability();

DROP TRIGGER IF EXISTS trg_protect_daily_report_entries ON public.daily_report_entries;
CREATE TRIGGER trg_protect_daily_report_entries
    BEFORE INSERT OR UPDATE OR DELETE ON public.daily_report_entries
    FOR EACH ROW
    EXECUTE FUNCTION public.check_submitted_report_immutability();

-- ============================================================================
-- 6. ATOMIC FINAL SUBMISSION RPC FUNCTION
-- ============================================================================

CREATE OR REPLACE FUNCTION public.submit_daily_report(p_report_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID;
    v_current_status TEXT;
    v_calculated_reels INTEGER := 0;
    v_updated_report JSONB;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    -- Confirm report ownership and status
    SELECT status INTO v_current_status
    FROM public.daily_reports
    WHERE id = p_report_id AND user_id = v_user_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Daily report not found or unauthorized.';
    END IF;

    IF v_current_status = 'SUBMITTED' THEN
        RAISE EXCEPTION 'This daily report has already been submitted and is locked.';
    END IF;

    -- Calculate total reels from all entries for this report
    SELECT COALESCE(SUM(cardinality(reel_codes)), 0) INTO v_calculated_reels
    FROM public.daily_report_entries
    WHERE report_id = p_report_id;

    -- Update report atomically to SUBMITTED
    UPDATE public.daily_reports
    SET status = 'SUBMITTED',
        total_reels = v_calculated_reels,
        submitted_at = timezone('utc'::text, now()),
        updated_at = timezone('utc'::text, now())
    WHERE id = p_report_id AND user_id = v_user_id;

    SELECT to_jsonb(r.*) INTO v_updated_report
    FROM public.daily_reports r
    WHERE r.id = p_report_id;

    RETURN v_updated_report;
END;
$$;

-- ============================================================================
-- 7. ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================

ALTER TABLE public.clients ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_report_entries ENABLE ROW LEVEL SECURITY;

-- CLIENTS POLICIES
DROP POLICY IF EXISTS "Users can read own clients" ON public.clients;
CREATE POLICY "Users can read own clients"
    ON public.clients FOR SELECT
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert own clients" ON public.clients;
CREATE POLICY "Users can insert own clients"
    ON public.clients FOR INSERT
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own clients" ON public.clients;
CREATE POLICY "Users can update own clients"
    ON public.clients FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Cannot delete client if they have report entries
DROP POLICY IF EXISTS "Users can delete unreferenced clients" ON public.clients;
CREATE POLICY "Users can delete unreferenced clients"
    ON public.clients FOR DELETE
    USING (
        auth.uid() = user_id AND
        NOT EXISTS (SELECT 1 FROM public.daily_report_entries WHERE client_id = clients.id)
    );

-- DAILY REPORTS POLICIES
DROP POLICY IF EXISTS "Users can read own daily reports" ON public.daily_reports;
CREATE POLICY "Users can read own daily reports"
    ON public.daily_reports FOR SELECT
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert own daily reports" ON public.daily_reports;
CREATE POLICY "Users can insert own daily reports"
    ON public.daily_reports FOR INSERT
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own draft daily reports" ON public.daily_reports;
CREATE POLICY "Users can update own draft daily reports"
    ON public.daily_reports FOR UPDATE
    USING (auth.uid() = user_id AND status = 'DRAFT')
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own draft daily reports" ON public.daily_reports;
CREATE POLICY "Users can delete own draft daily reports"
    ON public.daily_reports FOR DELETE
    USING (auth.uid() = user_id AND status = 'DRAFT');

-- DAILY REPORT ENTRIES POLICIES
DROP POLICY IF EXISTS "Users can read entries of own reports" ON public.daily_report_entries;
CREATE POLICY "Users can read entries of own reports"
    ON public.daily_report_entries FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.daily_reports
            WHERE daily_reports.id = daily_report_entries.report_id
            AND daily_reports.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Users can insert entries into own draft reports" ON public.daily_report_entries;
CREATE POLICY "Users can insert entries into own draft reports"
    ON public.daily_report_entries FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.daily_reports
            WHERE daily_reports.id = daily_report_entries.report_id
            AND daily_reports.user_id = auth.uid()
            AND daily_reports.status = 'DRAFT'
        )
    );

DROP POLICY IF EXISTS "Users can update entries in own draft reports" ON public.daily_report_entries;
CREATE POLICY "Users can update entries in own draft reports"
    ON public.daily_report_entries FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM public.daily_reports
            WHERE daily_reports.id = daily_report_entries.report_id
            AND daily_reports.user_id = auth.uid()
            AND daily_reports.status = 'DRAFT'
        )
    );

DROP POLICY IF EXISTS "Users can delete entries from own draft reports" ON public.daily_report_entries;
CREATE POLICY "Users can delete entries from own draft reports"
    ON public.daily_report_entries FOR DELETE
    USING (
        EXISTS (
            SELECT 1 FROM public.daily_reports
            WHERE daily_reports.id = daily_report_entries.report_id
            AND daily_reports.user_id = auth.uid()
            AND daily_reports.status = 'DRAFT'
        )
    );
