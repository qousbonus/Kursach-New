ALTER TABLE public.products
  ADD COLUMN low_stock_threshold integer NOT NULL DEFAULT 5
  CONSTRAINT products_low_stock_threshold_nonnegative CHECK (low_stock_threshold >= 0);

COMMENT ON COLUMN public.products.low_stock_threshold IS
  'Notify when a known stock quantity is at or below this nonnegative threshold; default 5, zero means out-of-stock only.';

CREATE VIEW public.low_stock_products
WITH (security_invoker = true)
AS
SELECT p.id, p.name, p.brand, p.image_url,
       p.quantity_in_stock, p.low_stock_threshold,
       CASE WHEN p.quantity_in_stock = 0
            THEN 'out_of_stock'::text ELSE 'low_stock'::text END AS severity
FROM public.products AS p
WHERE p.is_available IS TRUE
  AND p.quantity_in_stock IS NOT NULL
  AND p.quantity_in_stock <= p.low_stock_threshold;

REVOKE ALL ON TABLE public.low_stock_products FROM PUBLIC, anon, authenticated;
GRANT SELECT ON TABLE public.low_stock_products TO authenticated;
COMMENT ON VIEW public.low_stock_products IS
  'Current stock alerts for active products. Invoker permissions and base-table RLS apply. NULL stock is not treated as zero. UI role checks do not replace server authorization.';