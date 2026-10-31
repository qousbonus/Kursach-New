CREATE FUNCTION public.get_popular_products(p_limit integer DEFAULT 6)
RETURNS TABLE (
  product_id bigint,
  name text,
  brand text,
  image_url text,
  price numeric,
  discount_percent integer,
  quantity_in_stock integer,
  sold_quantity bigint
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $fn$
  WITH sales AS (
    SELECT oi.product_id, SUM(oi.quantity) AS sold_quantity
    FROM public.order_items AS oi
    JOIN public.orders AS o ON o.id = oi.order_id
    WHERE o.status = 'delivered'
    GROUP BY oi.product_id
  )
  SELECT p.id, p.name::text, p.brand, p.image_url::text,
         p.price, p.discount_percent, p.quantity_in_stock, s.sold_quantity
  FROM sales AS s
  JOIN public.products AS p ON p.id = s.product_id
  WHERE p.is_available IS TRUE
  ORDER BY s.sold_quantity DESC, p.id ASC
  LIMIT GREATEST(1, LEAST(COALESCE(p_limit, 6), 12));
$fn$;

REVOKE ALL ON FUNCTION public.get_popular_products(integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_popular_products(integer) TO anon, authenticated;
COMMENT ON FUNCTION public.get_popular_products(integer) IS
  'Public catalog aggregate: units in delivered orders, all time; no customer or individual order data. Default 6 items, maximum 12.';