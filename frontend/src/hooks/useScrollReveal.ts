import { useEffect, useRef } from 'react'

interface ScrollRevealOptions {
  /** Phần trăm element phải vào viewport trước khi trigger (0–1). Default 0.1 */
  threshold?: number
  /** Class thêm vào khi element vào viewport. Default 'revealed' */
  revealClass?: string
  /** Chỉ reveal 1 lần, không un-reveal khi scroll lên. Default true */
  once?: boolean
}

/**
 * Hook dùng IntersectionObserver để reveal element khi vào viewport.
 * Luôn dùng hook này thay cho window.addEventListener('scroll') —
 * scroll event gây reflow liên tục và làm giảm hiệu suất mobile.
 *
 * @example
 * const ref = useScrollReveal<HTMLDivElement>()
 * return <div ref={ref} className="scroll-reveal">...</div>
 *
 * // Với stagger delay cho list:
 * const ref = useScrollReveal<HTMLUListElement>()
 * return (
 *   <ul ref={ref}>
 *     {items.map((item, i) => (
 *       <li key={item.id} style={{ '--stagger-index': i } as React.CSSProperties}>
 *         {item.name}
 *       </li>
 *     ))}
 *   </ul>
 * )
 */
export function useScrollReveal<T extends Element = HTMLDivElement>(
  options: ScrollRevealOptions = {}
) {
  const { threshold = 0.1, revealClass = 'revealed', once = true } = options
  const ref = useRef<T>(null)

  useEffect(() => {
    const el = ref.current
    if (!el) return

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          el.classList.add(revealClass)
          if (once) observer.unobserve(el)
        } else if (!once) {
          el.classList.remove(revealClass)
        }
      },
      { threshold }
    )

    observer.observe(el)
    return () => observer.disconnect()
  }, [threshold, revealClass, once])

  return ref
}

/**
 * Reveal nhiều children cùng lúc với stagger delay.
 * Gắn ref vào container — mỗi direct child sẽ được reveal theo thứ tự.
 *
 * @example
 * const ref = useStaggerReveal<HTMLDivElement>()
 * return (
 *   <div ref={ref} className="stagger-container">
 *     <StadiumCard />
 *     <StadiumCard />
 *     <StadiumCard />
 *   </div>
 * )
 */
export function useStaggerReveal<T extends Element = HTMLDivElement>(
  options: ScrollRevealOptions = {}
) {
  const { threshold = 0.05, once = true } = options
  const ref = useRef<T>(null)

  useEffect(() => {
    const container = ref.current
    if (!container) return

    const children = Array.from(container.children) as HTMLElement[]
    children.forEach((child, i) => {
      child.style.setProperty('--stagger-index', String(i))
      child.classList.add('stagger-child')
    })

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          container.classList.add('stagger-revealed')
          if (once) observer.unobserve(container)
        } else if (!once) {
          container.classList.remove('stagger-revealed')
        }
      },
      { threshold }
    )

    observer.observe(container)
    return () => observer.disconnect()
  }, [threshold, once])

  return ref
}
